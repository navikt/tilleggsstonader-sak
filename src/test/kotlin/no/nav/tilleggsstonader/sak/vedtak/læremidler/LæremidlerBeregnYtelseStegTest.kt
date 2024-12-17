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

    val AUG_1 = LocalDate.of(2024, 8, 1)
    val AUG_15 = LocalDate.of(2024, 8, 15)
    val JAN_1_NESTE_ÅR = LocalDate.of(2025, 1, 1)
    val APRIL_30_NESTE_ÅR = LocalDate.of(2025, 4, 30)

    val fagsak = fagsak(stønadstype = Stønadstype.LÆREMIDLER)
    val behandling = behandling(fagsak = fagsak)
    val stønadsperiode = stønadsperiode(
        behandlingId = behandling.id,
        fom = AUG_15,
        tom = APRIL_30_NESTE_ÅR,
    )
    val aktivitet = aktivitet(
        behandlingId = behandling.id,
        fom = AUG_15,
        tom = APRIL_30_NESTE_ÅR,
        faktaOgVurdering = faktaOgVurderingAktivitetLæremidler(),
    )

    @BeforeEach
    fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagre(behandling, opprettGrunnlagsdata = false)
        stønadsperiodeRepository.insert(stønadsperiode)
        vilkårperiodeRepository.insert(aktivitet)
    }

    @Test
    fun `skal splitte andeler i 2, en for høsten og en for våren som ikke har bekreftet sats ennå`() {
        val saksbehandling = testoppsettService.hentSaksbehandling(behandling.id)

        val vedtaksperiode = VedtaksperiodeDto(fom = AUG_15, tom = APRIL_30_NESTE_ÅR)
        val innvilgelse = InnvilgelseLæremidlerRequest(vedtaksperioder = listOf(vedtaksperiode))
        steg.utførSteg(saksbehandling, innvilgelse)

        val andeler = tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!
            .andelerTilkjentYtelse.sortedBy { it.fom }

        with(andeler[0]) {
            assertThat(fom).isEqualTo(AUG_1)
            assertThat(tom).isEqualTo(AUG_1)
            assertThat(beløp).isEqualTo(4375)
            assertThat(type).isEqualTo(TypeAndel.LÆREMIDLER_AAP)
            assertThat(statusIverksetting).isEqualTo(StatusIverksetting.UBEHANDLET)
            assertThat(satstype).isEqualTo(Satstype.DAG)
        }
        with(andeler[1]) {
            assertThat(fom).isEqualTo(JAN_1_NESTE_ÅR)
            assertThat(tom).isEqualTo(JAN_1_NESTE_ÅR)
            assertThat(beløp).isEqualTo(3500)
            assertThat(type).isEqualTo(TypeAndel.LÆREMIDLER_AAP)
            assertThat(statusIverksetting).isEqualTo(StatusIverksetting.VENTER_PÅ_SATS_ENDRING)
            assertThat(satstype).isEqualTo(Satstype.DAG)
        }
        assertThat(andeler).hasSize(2)
    }

    @Test
    fun `Skal utbetale første ukedag i måneden dersom første dagen i måneden er lørdag eller søndag`() {
        val saksbehandling = testoppsettService.hentSaksbehandling(behandling.id)

        val vedtaksperiode = VedtaksperiodeDto(fom = LocalDate.of(2024, 12, 1), tom = LocalDate.of(2024, 12, 31))
        steg.utførSteg(saksbehandling, InnvilgelseLæremidlerRequest(vedtaksperioder = listOf(vedtaksperiode)))

        val andeler = tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!
            .andelerTilkjentYtelse.sortedBy { it.fom }

        val mandagEtterFom = LocalDate.of(2024, 12, 2)
        with(andeler.single()) {
            assertThat(fom).isEqualTo(mandagEtterFom)
            assertThat(tom).isEqualTo(mandagEtterFom)
            assertThat(beløp).isEqualTo(875)
            assertThat(type).isEqualTo(TypeAndel.LÆREMIDLER_AAP)
            assertThat(statusIverksetting).isEqualTo(StatusIverksetting.UBEHANDLET)
            assertThat(satstype).isEqualTo(Satstype.DAG)
        }
    }
}
