package no.nav.tilleggsstonader.sak.vedtak.læremidler

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class LæremidlerBeregnYtelseStegTest(
    @Autowired
    val steg: LæremidlerBeregnYtelseSteg,
    @Autowired
    val repository: VedtakRepository,
    @Autowired
    val tilkjentYtelseRepository: TilkjentYtelseRepository,
    @Autowired
    val stønadsperiodeRepository: StønadsperiodeRepository,
    @Autowired
    val vilkårperiodeRepository: VilkårperiodeRepository,
) : IntegrationTest() {

    val fagsak = fagsak(stønadstype = Stønadstype.LÆREMIDLER)
    val behandling = behandling(fagsak = fagsak)

    @BeforeEach
    fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagre(behandling, opprettGrunnlagsdata = false)
    }

    @Test
    fun `skal splitte andeler i 2, en for høsten og en for våren som ikke har bekreftet sats ennå`() {
        val fom = LocalDate.of(2025, 8, 15)
        val tom = LocalDate.of(2026, 4, 30)
        val datoUtbetalingDel1 = LocalDate.of(2025, 8, 15)
        val datoUtbetalingDel2 = LocalDate.of(2026, 1, 1)

        lagreAktivitetOgStønadsperiode(fom, tom)
        val saksbehandling = testoppsettService.hentSaksbehandling(behandling.id)

        val vedtaksperiode = VedtaksperiodeDto(fom = fom, tom = tom)
        val innvilgelse = InnvilgelseLæremidlerRequest(vedtaksperioder = listOf(vedtaksperiode))
        steg.utførSteg(saksbehandling, innvilgelse)

        val andeler = tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!
            .andelerTilkjentYtelse.sortedBy { it.fom }

        with(andeler[0]) {
            assertThat(this.fom).isEqualTo(datoUtbetalingDel1)
            assertThat(this.tom).isEqualTo(datoUtbetalingDel1)
            assertThat(beløp).isEqualTo(4505)
            assertThat(type).isEqualTo(TypeAndel.LÆREMIDLER_AAP)
            assertThat(statusIverksetting).isEqualTo(StatusIverksetting.UBEHANDLET)
            assertThat(satstype).isEqualTo(Satstype.DAG)
        }
        with(andeler[1]) {
            assertThat(this.fom).isEqualTo(datoUtbetalingDel2)
            assertThat(this.tom).isEqualTo(datoUtbetalingDel2)
            assertThat(beløp).isEqualTo(3500)
            assertThat(type).isEqualTo(TypeAndel.LÆREMIDLER_AAP)
            assertThat(statusIverksetting).isEqualTo(StatusIverksetting.VENTER_PÅ_SATS_ENDRING)
            assertThat(satstype).isEqualTo(Satstype.DAG)
        }
        assertThat(andeler).hasSize(2)
    }

    @Test
    fun `Skal utbetale første ukedag i måneden dersom første dagen i måneden er lørdag eller søndag`() {
        val fom = LocalDate.of(2024, 8, 1)
        val tom = LocalDate.of(2025, 4, 30)
        val mandagEtterFom = LocalDate.of(2024, 12, 2)

        lagreAktivitetOgStønadsperiode(fom, tom)
        val saksbehandling = testoppsettService.hentSaksbehandling(behandling.id)

        val vedtaksperiode = VedtaksperiodeDto(fom = LocalDate.of(2024, 12, 1), tom = LocalDate.of(2024, 12, 31))
        steg.utførSteg(saksbehandling, InnvilgelseLæremidlerRequest(vedtaksperioder = listOf(vedtaksperiode)))

        val andeler = tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!
            .andelerTilkjentYtelse.sortedBy { it.fom }
        with(andeler.single()) {
            assertThat(this.fom).isEqualTo(mandagEtterFom)
            assertThat(this.tom).isEqualTo(mandagEtterFom)
            assertThat(beløp).isEqualTo(875)
            assertThat(type).isEqualTo(TypeAndel.LÆREMIDLER_AAP)
            assertThat(statusIverksetting).isEqualTo(StatusIverksetting.UBEHANDLET)
            assertThat(satstype).isEqualTo(Satstype.DAG)
        }
    }

    fun lagreAktivitetOgStønadsperiode(fom: LocalDate, tom: LocalDate) {
        val stønadsperiode = stønadsperiode(
            behandlingId = behandling.id,
            fom = fom,
            tom = tom,
        )
        val aktivitet = aktivitet(
            behandlingId = behandling.id,
            fom = fom,
            tom = tom,
            faktaOgVurdering = faktaOgVurderingAktivitetLæremidler(),
        )
        stønadsperiodeRepository.insert(stønadsperiode)
        vilkårperiodeRepository.insert(aktivitet)
    }
}
