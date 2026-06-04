package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.juni
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.OpprettOpphør
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBeregningSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.DagligReiseBeregningService
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class DagligReiseBeregnYtelseStegIntegrationTest(
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val tilkjentYtelseService: TilkjentYtelseService,
    @Autowired private val dagligReiseBeregningService: DagligReiseBeregningService,
    @Autowired private val behandlingService: BehandlingService,
) : IntegrationTest() {
    @Test
    fun `skal kunne opphøre`() {
        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(Stønadstype.DAGLIG_REISE_TSO) {
                defaultDagligReiseTsoTestdata(2 januar 2025, 6 juni 2025)
            }

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(førstegangsbehandlingContext.behandlingId, tilSteg = StegType.SIMULERING) {
                vedtak {
                    opphør(opphørsdato = 15 mars 2025)
                }
            }

        val beregningsresultat = vedtakService.hentVedtak<OpphørDagligReise>(revurderingId).data.beregningsresultat
        assertThat(
            beregningsresultat.offentligTransport!!
                .reiser[0]
                .perioder
                .maxByOrNull {
                    it.grunnlag.tom
                }!!
                .grunnlag.tom,
        ).isEqualTo(14 mars 2025)

        val andelerFørstegangsbehandling =
            tilkjentYtelseService
                .hentForBehandling(
                    førstegangsbehandlingContext.behandlingId,
                ).andelerTilkjentYtelse
        val andelerOpphør = tilkjentYtelseService.hentForBehandling(revurderingId).andelerTilkjentYtelse

        assertThat(andelerOpphør.size).isNotEqualTo(andelerFørstegangsbehandling.size)
        assertThat(andelerOpphør.maxByOrNull { it.tom }!!.tom).isBeforeOrEqualTo(14 mars 2025)
    }

    @Test
    fun `skal bevare rammevedtak privat bil ved opphør`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true
        every { unleashService.isEnabled(Toggle.KAN_OPPHØRE_PRIVAT_BIL) } returns true

        val fom = 1 februar 2026
        val tom = 20 mars 2026
        val opphørsdato = 10 mars 2026

        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(Stønadstype.DAGLIG_REISE_TSO) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)
            }

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandlingContext.behandlingId,
                tilSteg = StegType.BEREGNE_YTELSE,
            ) {}

        gjennomførBeregningSteg(
            behandlingId = revurderingId,
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            opprettVedtak =
                OpprettOpphør(
                    årsaker = listOf(ÅrsakOpphør.ANNET),
                    begrunnelse = "begrunnelse",
                    opphørsdato = opphørsdato,
                ),
        )

        val opphørsvedtak = vedtakService.hentVedtak<OpphørDagligReise>(revurderingId).data
        assertThat(opphørsvedtak.rammevedtakPrivatBil).isNotNull
        assertThat(
            opphørsvedtak.rammevedtakPrivatBil!!
                .reiser
                .single()
                .grunnlag.tom,
        ).isEqualTo(opphørsdato.minusDays(1))
    }
}
