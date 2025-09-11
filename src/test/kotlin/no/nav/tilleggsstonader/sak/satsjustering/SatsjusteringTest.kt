package no.nav.tilleggsstonader.sak.satsjustering

import io.mockk.clearMocks
import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.kall.kjørSatsjusteringForStønadstype
import no.nav.tilleggsstonader.sak.kall.kjørSatsjusteringForStønadstypeKall
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.FaktaGrunnlagService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerBeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.vedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.SatsLæremidlerProvider
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.bekreftedeSatser
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerRequest
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppeLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class SatsjusteringTest : IntegrationTest() {
    @Autowired
    private lateinit var faktaGrunnlagService: FaktaGrunnlagService

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var vilkårsperiodeRepository: VilkårperiodeRepository

    @Autowired
    lateinit var satsLæremidlerProvider: SatsLæremidlerProvider

    @Autowired
    lateinit var læremidlerBeregnYtelseSteg: LæremidlerBeregnYtelseSteg

    val fom = LocalDate.of(2025, 8, 1)
    val tom = LocalDate.of(2026, 6, 30)

    @AfterEach
    override fun tearDown() {
        clearMocks(satsLæremidlerProvider)
    }

    @Test
    fun `skal justere sats i revurderinger som har tilkjent ytelse som venter på satsjustering`() {
        val behandling = opprettBehandlingMedAndelerTilSatsjustering()

        mockSatser()

        val behandlingerForSatsjustering =
            medBrukercontext(rolle = rolleConfig.utvikler) {
                kjørSatsjusteringForStønadstype(Stønadstype.LÆREMIDLER)
            }

        assertThat(behandlingerForSatsjustering).containsExactly(behandling.id)

        val sistIverksatteBehandling = behandlingRepository.finnSisteIverksatteBehandling(behandling.fagsakId)!!
        assertThat(behandling.id).isNotEqualTo(sistIverksatteBehandling.id)
        assertThat(sistIverksatteBehandling.forrigeIverksatteBehandlingId).isEqualTo(behandling.id)

        val tilkjentYtelseRevurdering = tilkjentYtelseRepository.findByBehandlingId(sistIverksatteBehandling.id)!!

        validerHarKopiertOverFaktagrunnlagFraForrigeBehandling(sistIverksatteBehandling)
        validerHarIngenAndelserSomVenterPåSatsEndring(sistIverksatteBehandling.id)
        assertThat(tilkjentYtelseRevurdering.andelerTilkjentYtelse)
            .noneMatch {
                it.statusIverksetting ==
                    StatusIverksetting.VENTER_PÅ_SATS_ENDRING
            }
    }

    @Test
    fun `kaller satsjustering-endepunkt uten utvikler-rolle, kaster feil`() {
        medBrukercontext {
            kjørSatsjusteringForStønadstypeKall(Stønadstype.LÆREMIDLER)
                .expectStatus()
                .isForbidden
        }
    }

    private fun mockSatser() {
        val nyMakssats = 10_000
        val ubekreftetSats = satsLæremidlerProvider.satser.first { !it.bekreftet }
        val nyUbekreftetSats =
            ubekreftetSats.copy(
                fom = ubekreftetSats.fom.plusYears(1),
                beløp =
                    ubekreftetSats.beløp
                        .map {
                            it.key to
                                nyMakssats
                        }.toMap(),
            )
        val nyBekreftetSats =
            ubekreftetSats.copy(
                tom =
                    ubekreftetSats.fom
                        .toYearMonth()
                        .withMonth(12)
                        .atEndOfMonth(),
                bekreftet = true,
                beløp =
                    ubekreftetSats.beløp
                        .map {
                            it.key to
                                nyMakssats
                        }.toMap(),
            )

        every {
            satsLæremidlerProvider.satser
        } returns bekreftedeSatser + nyBekreftetSats + nyUbekreftetSats
    }

    private fun opprettBehandlingMedAndelerTilSatsjustering(): Behandling {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(), stønadstype = Stønadstype.LÆREMIDLER)

        vilkårsperiodeRepository.insertAll(
            listOf(
                målgruppe(behandlingId = behandling.id, fom = fom, tom = tom, faktaOgVurdering = faktaOgVurderingMålgruppeLæremidler()),
                aktivitet(behandlingId = behandling.id, fom = fom, tom = tom, faktaOgVurdering = faktaOgVurderingAktivitetLæremidler()),
            ),
        )

        læremidlerBeregnYtelseSteg.utførSteg(
            saksbehandling = behandlingRepository.finnSaksbehandling(behandling.id),
            InnvilgelseLæremidlerRequest(
                vedtaksperioder = listOf(vedtaksperiodeDto(fom = fom, tom = tom)),
            ),
        )

        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!
        validerHarAndelerTilSatsjustering(tilkjentYtelse)

        return testoppsettService.ferdigstillBehandling(behandling)
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
        ).hasSize(1)
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
