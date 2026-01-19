package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.juni
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørDagligReise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class DagligReiseBeregnYtelseStegIntegrationTest(
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val iverksettService: IverksettService,
) : CleanDatabaseIntegrationTest() {
    @Test
    fun `skal kunne opphøre`() {
        val førstegangsbehandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(Stønadstype.DAGLIG_REISE_TSO) {
                defaultDagligReiseTsoTestdata(2 januar 2025, 6 juni 2025)
            }

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(førstegangsbehandlingId, tilSteg = StegType.SIMULERING) {
                vedtak {
                    opphør(opphørsdato = 15 mars 2025)
                }
            }

        val beregningsresultat = vedtakService.hentVedtak<OpphørDagligReise>(revurderingId)!!.data.beregningsresultat
        assertThat(
            beregningsresultat.offentligTransport!!
                .reiser[0]
                .perioder
                .maxByOrNull {
                    it.grunnlag.tom
                }!!
                .grunnlag.tom,
        ).isEqualTo(14 mars 2025)

        val andelerFørstegangsbehandling = iverksettService.hentAndelTilkjentYtelse(førstegangsbehandlingId)
        val andelerOpphør = iverksettService.hentAndelTilkjentYtelse(revurderingId)

        assertThat(andelerOpphør.size).isNotEqualTo(andelerFørstegangsbehandling.size)
        assertThat(andelerOpphør.maxByOrNull { it.tom }!!.tom).isBeforeOrEqualTo(14 mars 2025)
    }
}
