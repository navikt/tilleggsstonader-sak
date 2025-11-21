package no.nav.tilleggsstonader.sak.satsjustering

import io.mockk.clearMocks
import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.august
import no.nav.tilleggsstonader.libs.utils.dato.juni
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsessering
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.FaktaGrunnlagService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterBeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.vedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.SatsBoutgifterProvider
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.bekreftedeSatser
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.InnvilgelseBoutgifterRequest
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class SatsjusteringBoutgifterTest : CleanDatabaseIntegrationTest() {
    @Autowired
    private lateinit var faktaGrunnlagService: FaktaGrunnlagService

    @Autowired private lateinit var vilkårRepository: VilkårRepository

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var satsBoutgifterProvider: SatsBoutgifterProvider

    @Autowired
    lateinit var vilkårsperiodeRepository: VilkårperiodeRepository

    @Autowired
    lateinit var boutgifterBeregnYtelseSteg: BoutgifterBeregnYtelseSteg

    val sisteBekreftedeSatsÅr = bekreftedeSatser.maxOf { it.fom.year }
    val fom = 1.august(sisteBekreftedeSatsÅr)
    val tom = 30.juni(sisteBekreftedeSatsÅr + 1)

    @AfterEach
    fun resetMock() {
        clearMocks(satsBoutgifterProvider)
    }

    @Test
    fun `skal justere sats i revurderinger som har tilkjent ytelse som venter på satsjustering`() {
        val behandling = opprettBehandlingMedAndelerTilSatsjustering()

        mockSatser()

        val behandlingerForSatsjustering =
            medBrukercontext(roller = listOf(rolleConfig.utvikler)) {
                kall.satsjustering.satsjustering(Stønadstype.BOUTGIFTER)
            }

        kjørTasksKlareForProsessering()

        assertThat(behandlingerForSatsjustering).containsExactly(behandling.id)

        val satsjusteringBehandling = behandlingRepository.finnSisteIverksatteBehandling(behandling.fagsakId)!!
        assertThat(behandling.id).isNotEqualTo(satsjusteringBehandling.id)
        assertThat(satsjusteringBehandling.forrigeIverksatteBehandlingId).isEqualTo(behandling.id)

        validerHarKopiertOverFaktagrunnlagFraForrigeBehandling(satsjusteringBehandling)
        validerHarIngenAndelserSomVenterPåSatsEndring(satsjusteringBehandling.id)
    }

    @Test
    fun `kaller satsjustering-endepunkt, finnes behandling uten andeler til satsjustering, ingen behandlinger blir behandlet`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(), stønadstype = Stønadstype.BOUTGIFTER)
        lagreVilkårOgVedtak(behandling, fom = fom, tom = fom.toYearMonth().withMonth(12).atEndOfMonth())
        validerHarIngenAndelserSomVenterPåSatsEndring(behandling.id)
        testoppsettService.ferdigstillBehandling(behandling)

        val behandlingerTilSatsjustering =
            medBrukercontext(roller = listOf(rolleConfig.utvikler)) {
                kall.satsjustering.satsjustering(Stønadstype.BOUTGIFTER)
            }

        assertThat(behandlingerTilSatsjustering).isEmpty()
    }

    @Test
    fun `kaller satsjustering-endepunkt uten utvikler-rolle, kaster feil`() {
        medBrukercontext(roller = listOf(rolleConfig.beslutterRolle)) {
            kall.satsjustering.apiRespons
                .satsjustering(Stønadstype.BOUTGIFTER)
                .expectStatus()
                .isForbidden
        }
    }

    private fun mockSatser() {
        val nyMakssats = 10_000
        val ubekreftetSats = satsBoutgifterProvider.alleSatser.first { !it.bekreftet }
        val nyBekreftetSats =
            ubekreftetSats.copy(
                tom =
                    ubekreftetSats.fom
                        .toYearMonth()
                        .withMonth(12)
                        .atEndOfMonth(),
                bekreftet = true,
                beløp = nyMakssats,
            )
        val nyUbekreftetSats =
            ubekreftetSats.copy(
                fom = ubekreftetSats.fom.plusYears(1),
                beløp = nyMakssats,
            )

        every {
            satsBoutgifterProvider.alleSatser
        } returns bekreftedeSatser + nyBekreftetSats + nyUbekreftetSats
    }

    private fun opprettBehandlingMedAndelerTilSatsjustering(): Behandling {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(), stønadstype = Stønadstype.BOUTGIFTER)

        lagreVilkårOgVedtak(behandling, fom, tom)

        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!
        validerHarAndelerTilSatsjustering(tilkjentYtelse)

        return testoppsettService.ferdigstillBehandling(behandling)
    }

    private fun lagreVilkårOgVedtak(
        behandling: Behandling,
        fom: LocalDate,
        tom: LocalDate,
    ) {
        vilkårsperiodeRepository.insertAll(
            listOf(
                målgruppe(behandlingId = behandling.id, fom = fom, tom = tom, faktaOgVurdering = faktaOgVurderingMålgruppe()),
                aktivitet(behandlingId = behandling.id, fom = fom, tom = tom, faktaOgVurdering = faktaOgVurderingAktivitetBoutgifter()),
            ),
        )

        vilkårRepository.insertAll(
            listOf(
                vilkår(
                    behandlingId = behandling.id,
                    type = VilkårType.LØPENDE_UTGIFTER_EN_BOLIG,
                    fom = fom,
                    tom = tom,
                    utgift = 4000,
                ),
            ),
        )

        boutgifterBeregnYtelseSteg.utførSteg(
            saksbehandling = behandlingRepository.finnSaksbehandling(behandling.id),
            InnvilgelseBoutgifterRequest(
                vedtaksperioder = listOf(vedtaksperiodeDto(fom = fom, tom = tom)),
            ),
        )
    }

    private fun validerHarKopiertOverFaktagrunnlagFraForrigeBehandling(sistIverksatteBehandling: Behandling) {
        val grunnlagSistIverksatte = faktaGrunnlagService.hentGrunnlagsdata(sistIverksatteBehandling.id)
        val grunnlagForrigeBehandling =
            faktaGrunnlagService.hentGrunnlagsdata(sistIverksatteBehandling.forrigeIverksatteBehandlingId!!)

        assertThat(grunnlagSistIverksatte).isEqualTo(grunnlagForrigeBehandling)
    }

    private fun validerHarAndelerTilSatsjustering(tilkjentYtelse: TilkjentYtelse) {
        assertThat(
            tilkjentYtelse
                .andelerTilkjentYtelse
                .filter {
                    it.statusIverksetting ==
                        StatusIverksetting.VENTER_PÅ_SATS_ENDRING
                },
        ).hasSize(6)
    }

    private fun validerHarIngenAndelserSomVenterPåSatsEndring(behandlingId: BehandlingId) {
        val tilkjentYtelseRevurdering = tilkjentYtelseRepository.findByBehandlingId(behandlingId)!!
        assertThat(tilkjentYtelseRevurdering.andelerTilkjentYtelse)
            .noneMatch {
                it.statusIverksetting ==
                    StatusIverksetting.VENTER_PÅ_SATS_ENDRING
            }
    }
}
