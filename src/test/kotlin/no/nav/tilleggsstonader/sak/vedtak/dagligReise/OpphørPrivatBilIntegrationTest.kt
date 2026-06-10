package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectProblemDetail
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførKjørelisteBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.OpphørDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

class OpphørPrivatBilIntegrationTest(
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val tilkjentYtelseService: TilkjentYtelseService,
) : IntegrationTest() {
    @Test
    fun `skal kutte rammevedtak og innsendte kjørelister i opphør`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true
        every { unleashService.isEnabled(Toggle.KAN_OPPHØRE_PRIVAT_BIL) } returns true

        val fom = 2 februar 2026
        val tom = 20 mars 2026
        val opphørsdato = 9 mars 2026

        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(Stønadstype.DAGLIG_REISE_TSO) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)

                sendInnKjøreliste {
                    periode = Datoperiode(fom, tom)
                    kjørteDager =
                        listOf(
                            KjørtDag(dato = 2 februar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 9 februar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 16 februar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 23 februar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 2 februar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 9 februar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 16 februar 2026, parkeringsutgift = 50),
                        )
                }
            }

        val kjørelistebehandling =
            testoppsettService
                .hentBehandlinger(førstegangsbehandlingContext.fagsakId)
                .single { it.type == BehandlingType.KJØRELISTE }
        gjennomførKjørelisteBehandling(kjørelistebehandling)
        testoppsettService.settAndelerTilOkForBehandling(kjørelistebehandling.id)

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandlingContext.behandlingId,
                erRevurderingDagligReiseMedPrivatBil = true,
            ) {
                vedtak {
                    opphør(opphørsdato = opphørsdato)
                }
            }

        val opphørsvedtak = vedtakService.hentVedtak<OpphørDagligReise>(revurderingId).data

        val rammevedtak = opphørsvedtak.rammevedtakPrivatBil
        assertThat(rammevedtak).isNotNull
        assertThat(
            opphørsvedtak.rammevedtakPrivatBil!!
                .reiser
                .single()
                .grunnlag.tom,
        ).isEqualTo(opphørsdato.minusDays(1))

        val beregningsresultatPrivatBil = opphørsvedtak.beregningsresultat.privatBil
        assertThat(beregningsresultatPrivatBil).isNotNull
        assertThat(beregningsresultatPrivatBil!!.reiser).hasSize(1)

        val ukerIReise = beregningsresultatPrivatBil.reiser.single().perioder
        assertThat(ukerIReise).hasSize(5)
        assertThat(ukerIReise.maxOf { it.tom }).isEqualTo(opphørsdato.minusDays(1))

        val andeler = tilkjentYtelseService.hentForBehandling(revurderingId).andelerTilkjentYtelse
        assertThat(andeler.maxOf { it.fom }).isBefore(opphørsdato.minusDays(1))
    }

    @Test
    fun `skal kaste feil dersom opphør ikke skjer på en mandag`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true
        every { unleashService.isEnabled(Toggle.KAN_OPPHØRE_PRIVAT_BIL) } returns true

        val fom = 2 februar 2026
        val tom = 20 mars 2026
        val opphørsdato = 7 mars 2026

        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(Stønadstype.DAGLIG_REISE_TSO) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)
            }

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandlingContext.behandlingId,
                tilSteg = StegType.BEREGNE_YTELSE,
            ) {}

        kall.vedtak.apiRespons
            .lagreOpphør(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
                behandlingId = revurderingId,
                opphørDto =
                    OpphørDagligReiseRequest(
                        opphørsdato = opphørsdato,
                        årsakerOpphør = listOf(ÅrsakOpphør.ANNET),
                        begrunnelse = "Begrunnelse for opphør",
                    ),
            ).expectProblemDetail(HttpStatus.BAD_REQUEST, "Sett opphørsdato til en mandag")
    }
}
