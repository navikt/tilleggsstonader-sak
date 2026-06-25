package no.nav.tilleggsstonader.sak.behandling.vent

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.GenererPdfRequest
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.BehandlingContext
import no.nav.tilleggsstonader.sak.integrasjonstest.MINIMALT_BREV
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectProblemDetail
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tilordneÅpenBehandlingOppgaveForBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførKjørelisteBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.sendInnKjøreliste
import no.nav.tilleggsstonader.sak.util.KjørelisteSkjemaUtil.kjørelisteSkjema
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class KjørelistePåVentIntegrationTest : CleanDatabaseIntegrationTest() {
    private val fomUke1 = 5 januar 2026
    private val tomUke1 = 11 januar 2026
    private val fomUke2 = 12 januar 2026
    private val tomUke2 = 18 januar 2026

    @BeforeEach
    fun setUp() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true
    }

    @Test
    fun `skal sette nye kjørelistebehandlinger på vent om det finnes åpen kjørelistebehandling`() {
        testBrukerkontekst =
            TestBrukerKontekst(
                defaultBruker = "julenissen",
                defaultRoller = listOf(rolleConfig.beslutterRolle),
            )

        val context =
            opprettBehandlingOgGjennomførBehandlingsløp(stønadstype = Stønadstype.DAGLIG_REISE_TSO) {
                defaultDagligReisePrivatBilTsoTestdata(fomUke1, tomUke2)
                sendInnKjøreliste {
                    periode = Datoperiode(fomUke1, tomUke1)
                    kjørteDager = listOf(KjørtDag(dato = 5 januar 2026, parkeringsutgift = 50))
                }
            }

        val fagsakId = testoppsettService.hentBehandling(context.behandlingId).fagsakId
        val kjørelisteBehandling1 =
            testoppsettService.hentBehandlinger(fagsakId).single { it.type == BehandlingType.KJØRELISTE }

        // Påbegynt behandling skal ikke gjenbrukes når ny kjøreliste kommer inn
        tilordneÅpenBehandlingOppgaveForBehandling(kjørelisteBehandling1.id)
        sendInnNyKjørelisteSomSettesIKø(context.ident)

        val kjørelisteBehandling2 =
            testoppsettService
                .hentBehandlinger(fagsakId)
                .single { it.type == BehandlingType.KJØRELISTE && it.status == BehandlingStatus.SATT_PÅ_VENT }

        gjennomførKjørelisteBehandling(kjørelisteBehandling1)

        // Ny kjørelistebehandling tas av vent og skal nullstilles med oppdatert grunnlag
        tilordneÅpenBehandlingOppgaveForBehandling(kjørelisteBehandling2.id)
        kall.settPaVent.taAvVent(kjørelisteBehandling2.id, TaAvVentDto())

        val nullstiltBehandling = testoppsettService.hentBehandling(kjørelisteBehandling2.id)
        assertThat(nullstiltBehandling.forrigeIverksatteBehandlingId).isEqualTo(kjørelisteBehandling1.id)
        assertThat(nullstiltBehandling.type).isEqualTo(BehandlingType.KJØRELISTE)
        assertThat(nullstiltBehandling.steg).isEqualTo(StegType.KJØRELISTE)
        assertThat(nullstiltBehandling.status).isEqualTo(BehandlingStatus.UTREDES)
    }

    @Test
    fun `skal hindre send til beslutter når det finnes kjørelistebehandling på vent`() {
        val context = opprettInnvilgetDagligReiseMedGjennomførtKjøreliste()
        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = context.behandlingId,
                tilSteg = StegType.SEND_TIL_BESLUTTER,
            ) {
                vedtak { opphør(opphørsdato = fomUke2) }
            }

        sendInnNyKjørelisteSomSettesIKø(context.ident)

        kall.brev.genererPdf(revurderingId, GenererPdfRequest(MINIMALT_BREV))
        kall.brevmottakere.hent(revurderingId)
        kall.totrinnskontroll.apiRespons
            .sendTilBeslutter(revurderingId)
            .expectProblemDetail(HttpStatus.BAD_REQUEST, "Det finnes en kjørelistebehandling på vent")
    }

    @Test
    fun `skal hindre beslutte vedtak når det finnes kjørelistebehandling på vent`() {
        val context = opprettInnvilgetDagligReiseMedGjennomførtKjøreliste()
        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = context.behandlingId,
                tilSteg = StegType.BESLUTTE_VEDTAK,
            ) {
                vedtak { opphør(opphørsdato = fomUke2) }
            }

        sendInnNyKjørelisteSomSettesIKø(context.ident)

        medBrukercontext(bruker = "nissemor", roller = listOf(rolleConfig.beslutterRolle)) {
            tilordneÅpenBehandlingOppgaveForBehandling(revurderingId)
            kall.totrinnskontroll.apiRespons
                .beslutteVedtak(revurderingId, BeslutteVedtakDto(godkjent = true))
                .expectProblemDetail(HttpStatus.BAD_REQUEST, "Det finnes en kjørelistebehandling på vent")
        }
    }

    private fun opprettInnvilgetDagligReiseMedGjennomførtKjøreliste(): BehandlingContext {
        val context =
            opprettBehandlingOgGjennomførBehandlingsløp(stønadstype = Stønadstype.DAGLIG_REISE_TSO) {
                defaultDagligReisePrivatBilTsoTestdata(fomUke1, tomUke2)
                sendInnKjøreliste {
                    periode = Datoperiode(fomUke1, tomUke1)
                    kjørteDager = listOf(KjørtDag(dato = 5 januar 2026, parkeringsutgift = 50))
                }
            }
        val fagsakId = testoppsettService.hentBehandling(context.behandlingId).fagsakId
        val kjørelisteBehandling =
            testoppsettService.hentBehandlinger(fagsakId).single { it.type == BehandlingType.KJØRELISTE }
        gjennomførKjørelisteBehandling(kjørelisteBehandling)
        return context
    }

    private fun sendInnNyKjørelisteSomSettesIKø(ident: String) {
        val reiseId =
            kall.privatBil
                .hentRammevedtak(ident)
                .first()
                .reiseId
        sendInnKjøreliste(
            kjørelisteSkjema(
                reiseId = reiseId,
                periode = Datoperiode(fomUke2, tomUke2),
                dagerKjørt = listOf(KjørtDag(dato = 12 januar 2026, parkeringsutgift = 50)),
            ),
            ident = ident,
        )
    }
}
