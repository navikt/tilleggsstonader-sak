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
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingKategori
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.BrevService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.database.Fil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveDomain
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.FerdigstillOppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.OpprettOppgaveTask
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.clearBrukerContext
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.mockBrukerContext
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtaksresultatService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.ÅrsakUnderkjent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Properties
import java.util.UUID

class BeslutteVedtakStegTest {

    private val taskService = mockk<TaskService>()
    private val fagsakService = mockk<FagsakService>()
    private val totrinnskontrollService = mockk<TotrinnskontrollService>(relaxed = true)
    private val oppgaveService = mockk<OppgaveService>()

    // private val iverksettingDtoMapper = mockk<IverksettingDtoMapper>()
    // private val iverksett = mockk<IverksettClient>()
    private val vedtaksresultatService = mockk<VedtaksresultatService>()
    private val brevService = mockk<BrevService>()
    private val behandlingService = mockk<BehandlingService>()

    private val beslutteVedtakSteg = BeslutteVedtakSteg(
        taskService = taskService,
        fagsakService = fagsakService,
        oppgaveService = oppgaveService,
        totrinnskontrollService = totrinnskontrollService,
        behandlingService = behandlingService,
        vedtaksresultatService = vedtaksresultatService,
        brevService = brevService,
    )

    private val innloggetBeslutter = "sign2"

    private val fagsak = fagsak(
        stønadstype = Stønadstype.BARNETILSYN,
        identer = setOf(PersonIdent(ident = "12345678901")),
    )
    private val behandlingId = UUID.randomUUID()

    private val oppgave = OppgaveDomain(
        id = UUID.randomUUID(),
        behandlingId = behandlingId,
        gsakOppgaveId = 123L,
        type = Oppgavetype.BehandleSak,
        erFerdigstilt = false,
    )
    private lateinit var taskSlot: MutableList<Task>

    @BeforeEach
    internal fun setUp() {
        mockBrukerContext(innloggetBeslutter)
        taskSlot = mutableListOf()
        every { fagsakService.hentAktivIdent(any()) } returns fagsak.hentAktivIdent()
        every { fagsakService.fagsakMedOppdatertPersonIdent(any()) } returns fagsak
        every { taskService.save(capture(taskSlot)) } returns Task("", "", Properties())
        every { oppgaveService.hentOppgaveSomIkkeErFerdigstilt(any(), any()) } returns oppgave
        // every { iverksettingDtoMapper.tilDto(any(), any()) } returns mockk()
        // every { iverksett.iverksett(any(), any()) } just Runs
        // every { iverksett.iverksettUtenBrev(any()) } just Runs
        every { vedtaksresultatService.hentVedtaksresultat(any()) } returns TypeVedtak.INNVILGET
        // every { vedtaksresultatService.oppdaterBeslutter(any(), any()) } just Runs
        every { behandlingService.oppdaterResultatPåBehandling(any(), any()) } answers {
            behandling(fagsak, id = behandlingId, resultat = secondArg())
        }
    }

    @AfterEach
    internal fun tearDown() {
        clearBrukerContext()
    }

    @Test
    internal fun `skal opprette iverksettMotOppdragTask etter beslutte vedtak hvis godkjent`() {
        every { brevService.lagEndeligBeslutterbrev(any()) } returns Fil("123".toByteArray())

        val nesteSteg = utførTotrinnskontroll(godkjent = true)

        assertThat(nesteSteg).isEqualTo(StegType.JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV)
        assertThat(taskSlot[0].type).isEqualTo(FerdigstillOppgaveTask.TYPE)
        // assertThat(taskSlot[1].type).isEqualTo(PollStatusFraIverksettTask.TYPE)
        // assertThat(taskSlot[2].type).isEqualTo(BehandlingsstatistikkTask.TYPE)
        // assertThat(objectMapper.readValue<BehandlingsstatistikkTaskPayload>(taskSlot[2].payload).hendelse)
        //    .isEqualTo(Hendelse.BESLUTTET)
        verify(exactly = 1) {
            behandlingService.oppdaterResultatPåBehandling(
                behandlingId,
                BehandlingResultat.INNVILGET,
            )
        }
        // verify(exactly = 1) { iverksett.iverksett(any(), any()) }
        // verify(exactly = 0) { iverksett.iverksettUtenBrev(any()) }
    }

    @Test
    internal fun `skal opprette opprettBehandleUnderkjentVedtakOppgave etter beslutte vedtak hvis underkjent`() {
        val nesteSteg = utførTotrinnskontroll(
            godkjent = false,
            begrunnelse = "begrunnelse",
            årsakerUnderkjent = listOf(ÅrsakUnderkjent.AKTIVITET),
        )

        val taskData = objectMapper.readValue<OpprettOppgaveTask.OpprettOppgaveTaskData>(taskSlot[1].payload)

        assertThat(nesteSteg).isEqualTo(StegType.SEND_TIL_BESLUTTER)
        assertThat(taskSlot[0].type).isEqualTo(FerdigstillOppgaveTask.TYPE)
        assertThat(taskSlot[1].type).isEqualTo(OpprettOppgaveTask.TYPE)
        assertThat(taskData.oppgave.oppgavetype).isEqualTo(Oppgavetype.BehandleUnderkjentVedtak)
    }

    @Test
    internal fun `Skal kaste feil når behandling allerede er iverksatt `() {
        val behandling = behandling(fagsak, BehandlingStatus.IVERKSETTER_VEDTAK, StegType.JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV)
        val apiFeil =
            catchThrowableOfType<ApiFeil> { beslutteVedtakSteg.validerSteg(saksbehandling(behandling = behandling)) }
        assertThat(apiFeil.message).isEqualTo("Behandlingen er allerede besluttet. Status på behandling er 'Iverksetter vedtak'")
    }

    private fun utførTotrinnskontroll(
        godkjent: Boolean,
        saksbehandling: Saksbehandling = opprettSaksbehandling(),
        begrunnelse: String? = null,
        årsakerUnderkjent: List<ÅrsakUnderkjent> = emptyList(),
    ): StegType {
        return beslutteVedtakSteg.utførOgReturnerNesteSteg(
            saksbehandling,
            BeslutteVedtakDto(godkjent = godkjent, begrunnelse = begrunnelse, årsakerUnderkjent = årsakerUnderkjent),
        )
    }

    private fun opprettSaksbehandling(årsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD) =
        saksbehandling(
            fagsak,
            behandling(
                fagsak = fagsak,
                id = behandlingId,
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                status = BehandlingStatus.FATTER_VEDTAK,
                steg = beslutteVedtakSteg.stegType(),
                resultat = BehandlingResultat.IKKE_SATT,
                årsak = årsak,
                kategori = BehandlingKategori.NASJONAL,
            ),
        )
}
