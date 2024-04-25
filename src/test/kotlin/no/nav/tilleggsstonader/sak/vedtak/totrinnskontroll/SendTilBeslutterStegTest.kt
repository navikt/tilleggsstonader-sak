package no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat.INNVILGET
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.brev.Vedtaksbrev
import no.nav.tilleggsstonader.sak.brev.VedtaksbrevRepository
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.FerdigstillOppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.OpprettOppgaveTask
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.clearBrukerContext
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.mockBrukerContext
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.oppgave
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtaksresultatService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnInternStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnskontrollUtil.totrinnskontroll
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
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
    private val vedtaksresultatService = mockk<VedtaksresultatService>()
    private val vilkårService = mockk<VilkårService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val totrinnskontrollService = mockk<TotrinnskontrollService>(relaxed = true)

    private val beslutteVedtakSteg =
        SendTilBeslutterSteg(
            taskService,
            behandlingService,
            vedtaksbrevRepository,
            vedtaksresultatService,
            vilkårService,
            oppgaveService,
            totrinnskontrollService,
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
        every { oppgaveService.hentBehandleSakOppgaveSomIkkeErFerdigstilt(any()) } returns oppgave(behandling.id)
        every { oppgaveService.hentOppgaveSomIkkeErFerdigstilt(any(), any()) } returns null
        every { oppgaveService.hentOppgave(any()) } returns Oppgave(id = 123, versjon = 0)

        // TODO tilbakekreving
        // every { simuleringService.hentLagretSimuleringsoppsummering(any()) } returns simuleringsoppsummering
        // every { tilbakekrevingService.harSaksbehandlerTattStillingTilTilbakekreving(any()) } returns true
        // every { tilbakekrevingService.finnesÅpenTilbakekrevingsBehandling(any()) } returns true

        every { vedtaksresultatService.hentVedtaksresultat(any()) } returns TypeVedtak.INNVILGET
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
        every { totrinnskontrollService.hentTotrinnskontroll(any()) } returns totrinnskontroll(status = TotrinnInternStatus.UNDERKJENT)

        utførOgVerifiserKall(Oppgavetype.BehandleUnderkjentVedtak)
        verifiserVedtattBehandlingsstatistikkTask()
    }

    @Test
    internal fun `Skal kaste feil hvis oppgave med type BehandleUnderkjentVedtak eller BehandleSak ikke finnes`() {
        every { oppgaveService.hentBehandleSakOppgaveSomIkkeErFerdigstilt(any()) } returns null

        val feil = catchThrowableOfType<ApiFeil> { beslutteVedtakSteg.validerSteg(behandling) }
        assertThat(feil.feil)
            .contains("Oppgaven for behandlingen er ikke tilgjengelig.")
    }

    @Test
    internal fun `Skal kaste feil hvis BehandleSak-oppgaven er tilordnet en annen saksbehandler`() {
        val oppgaveId = 10099L
        val oppgaveDomain = oppgave(behandling.id, gsakOppgaveId = oppgaveId)
        val oppgave = Oppgave(id = oppgaveId, versjon = 0, tilordnetRessurs = "annenSaksbehandler")
        every { oppgaveService.hentBehandleSakOppgaveSomIkkeErFerdigstilt(any()) } returns oppgaveDomain
        every { oppgaveService.hentOppgave(oppgaveDomain.gsakOppgaveId) } returns oppgave

        val feil = catchThrowableOfType<ApiFeil> { beslutteVedtakSteg.validerSteg(behandling) }
        assertThat(feil.feil)
            .contains("Kan ikke sende til beslutter. Oppgaven for behandlingen er plukket av annenSaksbehandler")
    }

    @Test
    internal fun `Skal kaste feil hvis godkjenne vedtak-oppgaven ikke er ferdigstilt`() {
        every {
            oppgaveService.hentOppgaveSomIkkeErFerdigstilt(behandling.id, Oppgavetype.GodkjenneVedtak)
        } returns oppgave(behandling.id)

        val feil = catchThrowableOfType<ApiFeil> { beslutteVedtakSteg.validerSteg(behandling) }
        assertThat(feil.feil)
            .contains("Det finnes en Godkjenne Vedtak oppgave systemet må ferdigstille før behandlingen kan sendes til beslutter.")
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

    private fun utførOgVerifiserKall(ferdigstillOppgaveType: Oppgavetype) {
        mockBrukerContext("saksbehandlernavn")

        utførSteg()
        clearBrukerContext()

        verify { behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.FATTER_VEDTAK) }

        with(taskSlot[1]) {
            assertThat(type).isEqualTo(OpprettOppgaveTask.TYPE)
            val taskData = objectMapper.readValue<OpprettOppgaveTask.OpprettOppgaveTaskData>(payload)
            assertThat(taskData.oppgave.oppgavetype).isEqualTo(Oppgavetype.GodkjenneVedtak)
        }

        with(taskSlot[0]) {
            assertThat(type).isEqualTo(FerdigstillOppgaveTask.TYPE)
            assertThat(objectMapper.readValue<FerdigstillOppgaveTask.FerdigstillOppgaveTaskData>(payload).oppgavetype)
                .isEqualTo(ferdigstillOppgaveType)
        }
    }

    private fun utførSteg() {
        beslutteVedtakSteg.utførSteg(behandling, null)
    }
}
