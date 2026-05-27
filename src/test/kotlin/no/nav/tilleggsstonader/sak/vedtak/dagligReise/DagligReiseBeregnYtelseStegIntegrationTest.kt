package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.juni
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførKjørelisteBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.DagligReiseBeregningService
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørDagligReise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

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
                tilSteg = StegType.SIMULERING,
            ) {
                vedtak {
                    opphør(opphørsdato = opphørsdato)
                }
            }

        val opphørsvedtak = vedtakService.hentVedtak<OpphørDagligReise>(revurderingId).data
        assertThat(opphørsvedtak.rammevedtakPrivatBil).isNotNull
        assertThat(
            opphørsvedtak.rammevedtakPrivatBil!!
                .reiser
                .single()
                .grunnlag.tom,
        ).isEqualTo(9 mars 2026)
    }

    /**
     * TODO: Når andeler er tilpasset for opphør bør man heller stoppe revurderingen på SIMULERING-steg
     * og hente ut vedtaket som blir lagret ned.
     */
    @Test
    fun `opphør av privat bil-behandling etter kjøreliste skal bevare trimmet rammevedtak`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val fom = 5 januar 2026
        val tom = 18 januar 2026
        val opphørsdato = 7 januar 2026

        val førstegangsBehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(Stønadstype.DAGLIG_REISE_TSO) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)

                sendInnKjøreliste {
                    periode = Datoperiode(fom, 11 januar 2026)
                    kjørteDager =
                        listOf(
                            KjørtDag(dato = 5 januar 2026),
                            KjørtDag(dato = 6 januar 2026),
                        )
                }
            }

        val vedtaksperioderFørstegangsbehandling =
            vedtakService
                .hentVedtak<InnvilgelseDagligReise>(
                    førstegangsBehandlingContext.behandlingId,
                ).data.vedtaksperioder

        val kjørelisteBehandling =
            testoppsettService
                .hentBehandlinger(testoppsettService.hentBehandling(førstegangsBehandlingContext.behandlingId).fagsakId)
                .single { it.type == BehandlingType.KJØRELISTE }

        gjennomførKjørelisteBehandling(kjørelisteBehandling)

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsBehandlingContext.behandlingId,
                tilSteg = StegType.BEREGNE_YTELSE,
            ) {
                vedtak {
                    opphør(opphørsdato = opphørsdato)
                }
            }

        val saksbehandling = behandlingService.hentSaksbehandling(revurderingId)

        val res =
            dagligReiseBeregningService.beregn(
                vedtaksperioder = listOf(vedtaksperioderFørstegangsbehandling.first().copy(tom = opphørsdato.minusDays(1))),
                behandling = saksbehandling,
                beregningsplan =
                    Beregningsplan(
                        omfang = Beregningsomfang.FRA_DATO,
                        fraDato = LocalDate.now(),
                    ),
                typeVedtak = TypeVedtak.OPPHØR,
            )

        assertThat(res.rammevedtakPrivatBil).isNotNull

        val rammevedtakReise = res.rammevedtakPrivatBil!!.reiser.single()
        assertThat(rammevedtakReise.grunnlag.fom).isEqualTo(fom)
        assertThat(rammevedtakReise.grunnlag.tom).isEqualTo(opphørsdato.minusDays(1))
        assertThat(rammevedtakReise.grunnlag.delperioder).hasSize(1)
        assertThat(
            rammevedtakReise.grunnlag.delperioder
                .single()
                .tom,
        ).isEqualTo(opphørsdato.minusDays(1))
    }
}
