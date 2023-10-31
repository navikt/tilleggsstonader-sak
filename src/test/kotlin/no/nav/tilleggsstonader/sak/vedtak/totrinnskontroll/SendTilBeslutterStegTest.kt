package no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat.INNVILGET
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.Behandlingshistorikk
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.Vedtaksbrev
import no.nav.tilleggsstonader.sak.brev.VedtaksbrevRepository
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.FerdigstillOppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.OpprettOppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.OpprettOppgaveTask.OpprettOppgaveTaskData
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.clearBrukerContext
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.mockBrukerContext
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtaksresultatService
import no.nav.tilleggsstonader.sak.vilkår.VilkårService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.util.Properties
import java.util.UUID

class SendTilBeslutterStegTest {

    private val taskService = mockk<TaskService>()
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val vedtaksbrevRepository = mockk<VedtaksbrevRepository>()
    private val behandlingshistorikkService = mockk<BehandlingshistorikkService>()
    private val vedtaksresultatService = mockk<VedtaksresultatService>()
    private val vilkårService = mockk<VilkårService>()
    private val oppgaveService = mockk<OppgaveService>()

    private val beslutteVedtakSteg =
        SendTilBeslutterSteg(
            taskService,
            behandlingService,
            vedtaksbrevRepository,
            behandlingshistorikkService,
            vedtaksresultatService,
            vilkårService,
            oppgaveService,
        )
    private val fagsak = fagsak(
        stønadstype = Stønadstype.BARNETILSYN,
        identer = setOf(PersonIdent(ident = "12345678901")),
    )
    private val saksbehandlerNavn = "saksbehandlernavn"
    private val vedtaksbrev = Vedtaksbrev(
        behandlingId = UUID.randomUUID(),
        saksbehandlersignatur = saksbehandlerNavn,
        beslutterPdf = null,
        saksbehandlerIdent = saksbehandlerNavn,
        saksbehandlerHtml = "",
    )

    private val behandling = saksbehandling(
        fagsak,
        behandling(
            fagsak = fagsak,
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            status = BehandlingStatus.UTREDES,
            steg = beslutteVedtakSteg.stegType(),
            resultat = BehandlingResultat.IKKE_SATT,
            årsak = BehandlingÅrsak.SØKNAD,
        ),
    )

    private val revurdering =
        behandling.copy(type = BehandlingType.REVURDERING, resultat = INNVILGET)

    private lateinit var taskSlot: MutableList<Task>

    @BeforeEach
    internal fun setUp() {
        taskSlot = mutableListOf()
        every {
            taskService.save(capture(taskSlot))
        } returns Task("", "", Properties())

        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev

        every { vilkårService.erAlleVilkårOppfylt(any()) } returns true

        every { vedtaksbrevRepository.existsById(any()) } returns true
        every { oppgaveService.hentBehandleSakOppgaveSomIkkeErFerdigstilt(any()) } returns mockk()

        // TODO tilbakekreving
        // every { simuleringService.hentLagretSimuleringsoppsummering(any()) } returns simuleringsoppsummering
        // every { tilbakekrevingService.harSaksbehandlerTattStillingTilTilbakekreving(any()) } returns true
        // every { tilbakekrevingService.finnesÅpenTilbakekrevingsBehandling(any()) } returns true

        every { vedtaksresultatService.hentVedtaksresultat(any()) } returns TypeVedtak.INNVILGET
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), any()) } returns
            Behandlingshistorikk(behandlingId = UUID.randomUUID(), steg = StegType.SEND_TIL_BESLUTTER)
        mockBrukerContext(saksbehandlerNavn)
    }

    @AfterEach
    internal fun tearDown() {
        clearBrukerContext()
    }

    @Test
    internal fun `Innvilget behandling - alt ok`() {
        val innvilgetBehandling = behandling.copy(resultat = INNVILGET)
        beslutteVedtakSteg.validerSteg(innvilgetBehandling)
    }

    @Test
    internal fun `Innvilget behandling - IKKE ok hvis erAlleVilkårOppfylt false`() {
        every { vilkårService.erAlleVilkårOppfylt(any()) } returns false
        val innvilgetBehandling = behandling.copy(resultat = INNVILGET)
        val forvetetFeilmelding =
            "Kan ikke innvilge hvis ikke alle vilkår er oppfylt for behandlingId: ${innvilgetBehandling.id}"
        assertThat(catchThrowableOfType<ApiFeil> { beslutteVedtakSteg.validerSteg(innvilgetBehandling) }.feil)
            .isEqualTo(forvetetFeilmelding)
    }

    @Test
    internal fun `Skal avslutte oppgave BehandleSak hvis den finnes`() {
        utførOgVerifiserKall(Oppgavetype.BehandleSak)
        verifiserVedtattBehandlingsstatistikkTask()
    }

    @Test
    internal fun `Skal avslutte oppgave BehandleUnderkjentVedtak hvis den finnes`() {
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), StegType.BESLUTTE_VEDTAK) } returns
            Behandlingshistorikk(
                behandlingId = UUID.randomUUID(),
                steg = StegType.BESLUTTE_VEDTAK,
                utfall = StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT,
            )
        utførOgVerifiserKall(Oppgavetype.BehandleUnderkjentVedtak)
        verifiserVedtattBehandlingsstatistikkTask()
    }

    @Test
    internal fun `Skal kaste feil hvis oppgave med type BehandleUnderkjentVedtak eller BehandleSak ikke finnes`() {
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), StegType.BESLUTTE_VEDTAK) } returns
            Behandlingshistorikk(
                behandlingId = UUID.randomUUID(),
                steg = StegType.BESLUTTE_VEDTAK,
                utfall = StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT,
            )
        every { oppgaveService.hentBehandleSakOppgaveSomIkkeErFerdigstilt(any()) } returns null
        val feil = catchThrowableOfType<Feil> { beslutteVedtakSteg.validerSteg(behandling) }
        assertThat(feil.frontendFeilmelding)
            .contains("Oppgaven for behandlingen er ikke tilgjengelig. Vennligst vent og prøv igjen om litt.")
    }

    @Test
    internal fun `Skal feile hvis saksbehandlersignatur i vedtaksbrev er ulik saksbehandleren som sendte til beslutter`() {
        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev.copy(saksbehandlersignatur = "Saksbehandler A")
        mockBrukerContext("Saksbehandler B")

        assertThat(catchThrowableOfType<ApiFeil> { beslutteVedtakSteg.validerSteg(behandling) })
            .hasMessageContaining("En annen saksbehandler har signert vedtaksbrevet")
    }

    @Test
    internal fun `skal ikke hente brev når man håndterer behandling med årsak korrigering uten brev`() {
        every { vedtaksresultatService.hentVedtaksresultat(any()) } returns TypeVedtak.INNVILGET
        val behandling = behandling.copy(årsak = BehandlingÅrsak.KORRIGERING_UTEN_BREV)

        beslutteVedtakSteg.validerSteg(behandling)

        verify(exactly = 0) {
            vedtaksbrevRepository.findByIdOrNull(any())
        }
    }

    // TODO DVH
    private fun verifiserVedtattBehandlingsstatistikkTask() {
        // assertThat(taskSlot[2].type).isEqualTo(BehandlingsstatistikkTask.TYPE)
        // assertThat(objectMapper.readValue<BehandlingsstatistikkTaskPayload>(taskSlot[2].payload).hendelse)
        //    .isEqualTo(Hendelse.VEDTATT)
    }

    private fun utførOgVerifiserKall(oppgavetype: Oppgavetype) {
        mockBrukerContext("saksbehandlernavn")

        utførSteg()
        clearBrukerContext()

        verify { behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.FATTER_VEDTAK) }

        assertThat(taskSlot[0].type).isEqualTo(OpprettOppgaveTask.TYPE)
        assertThat(objectMapper.readValue<OpprettOppgaveTaskData>(taskSlot[0].payload).oppgavetype)
            .isEqualTo(Oppgavetype.GodkjenneVedtak)

        assertThat(taskSlot[1].type).isEqualTo(FerdigstillOppgaveTask.TYPE)
        assertThat(objectMapper.readValue<FerdigstillOppgaveTask.FerdigstillOppgaveTaskData>(taskSlot[1].payload).oppgavetype)
            .isEqualTo(oppgavetype)
    }

    private fun utførSteg() {
        beslutteVedtakSteg.utførSteg(behandling, null)
    }
}
