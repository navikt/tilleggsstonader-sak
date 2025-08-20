package no.nav.tilleggsstonader.sak.satsjustering

import io.mockk.clearMocks
import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerBeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.vedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.SatsLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.SatsLæremidlerService
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
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
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var vilkårsperiodeRepository: VilkårperiodeRepository

    @Autowired
    lateinit var satsLæremidlerService: SatsLæremidlerService

    @Autowired
    lateinit var læremidlerBeregnYtelseSteg: LæremidlerBeregnYtelseSteg

    @Autowired
    lateinit var finnBehandlingerForSatsjusteringService: FinnBehandlingerForSatsjusteringService

    val fom = LocalDate.of(2025, 8, 1)
    val tom = LocalDate.of(2026, 6, 30)

    @AfterEach
    override fun tearDown() {
        clearMocks(satsLæremidlerService)
    }

    @Test
    fun `skal justere sats i revurderinger som har tilkjent ytelse som venter på satsjustering`() {
        var behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(), stønadstype = Stønadstype.LÆREMIDLER)

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

        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingId(behandling.id)
        assertThat(
            tilkjentYtelse!!
                .andelerTilkjentYtelse
                .filter {
                    it.statusIverksetting ==
                        StatusIverksetting.VENTER_PÅ_SATS_ENDRING
                },
        ).hasSize(1)

        behandling = testoppsettService.ferdigstillBehandling(behandling)

        every {
            satsLæremidlerService.finnSatsForPeriode(
                match { it.fom > fom },
            )
        } returns
            SatsLæremidler(
                fom = tom.withMonth(1).withDayOfMonth(1),
                tom = LocalDate.MAX,
                beløp = mapOf(Studienivå.VIDEREGÅENDE to 1000, Studienivå.HØYERE_UTDANNING to 1500),
                bekreftet = true,
            )

        val behandlingerForSatsjustering = finnBehandlingerForSatsjusteringService.sjekkBehandlingerForSatsjustering(Stønadstype.LÆREMIDLER)
        assertThat(behandlingerForSatsjustering).containsExactly(behandling.id)

        // TODO: Kjør jobb for å justere sats

        val sistIverksatteBehandling = behandlingRepository.finnSisteIverksatteBehandling(behandling.fagsakId)!!
        assertThat(behandling.id).isNotEqualTo(sistIverksatteBehandling.id)
        assertThat(sistIverksatteBehandling.forrigeIverksatteBehandlingId).isEqualTo(behandling.id)

        with(tilkjentYtelseRepository.findByBehandlingId(sistIverksatteBehandling.id)!!) {
            assertThat(
                andelerTilkjentYtelse
                    .filter {
                        it.statusIverksetting ==
                            StatusIverksetting.VENTER_PÅ_SATS_ENDRING
                    },
            ).isEmpty()
        }
    }
}
