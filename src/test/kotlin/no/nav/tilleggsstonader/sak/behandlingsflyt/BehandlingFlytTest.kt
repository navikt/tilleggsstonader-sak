package no.nav.tilleggsstonader.sak.behandlingsflyt

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.prosessering.internal.TaskWorker
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.libs.log.IdUtils
import no.nav.tilleggsstonader.libs.log.mdc.MDCConstants
import no.nav.tilleggsstonader.libs.test.fnr.FnrGenerator
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.OpprettTestBehandlingController
import no.nav.tilleggsstonader.sak.behandling.TestBehandlingRequest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.brev.GenererPdfRequest
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerVedtaksbrevRepository
import no.nav.tilleggsstonader.sak.brev.brevmottaker.MottakerTestUtil.mottakerPerson
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerVedtaksbrev
import no.nav.tilleggsstonader.sak.brev.vedtaksbrev.BrevController
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveRepository
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.FerdigstillOppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.OpprettOppgaveTask
import no.nav.tilleggsstonader.sak.statistikk.task.BehandlingsstatistikkTask
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringStegService
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnVedtakController
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollController
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.TotrinnkontrollStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.ÅrsakUnderkjent
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårController
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OpprettVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelTestUtil.oppfylteDelvilkårPassBarnDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.dummyVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.dummyVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.test.context.transaction.TestTransaction
import java.time.LocalDate

class BehandlingFlytTest(
    @Autowired val barnService: BarnService,
    @Autowired val oppgaveRepository: OppgaveRepository,
    @Autowired val opprettTestBehandlingController: OpprettTestBehandlingController,
    @Autowired val vilkårController: VilkårController,
    @Autowired val tilsynBarnVedtakController: TilsynBarnVedtakController,
    @Autowired val brevController: BrevController,
    @Autowired val brevmottakereRepository: BrevmottakerVedtaksbrevRepository,
    @Autowired val totrinnskontrollController: TotrinnskontrollController,
    @Autowired val totrinnskontrollService: TotrinnskontrollService,
    @Autowired val taskService: TaskService,
    @Autowired val stegService: StegService,
    @Autowired val taskWorker: TaskWorker,
    @Autowired val vilkårperiodeService: VilkårperiodeService,
    @Autowired val stønadsperiodeService: StønadsperiodeService,
    @Autowired val simuleringStegService: SimuleringStegService,
) : IntegrationTest() {
    val personIdent = FnrGenerator.generer(år = 2000)

    @Test
    fun `saksbehandler innvilger vedtak og beslutter godkjenner vedtak`() {
        val behandlingId =
            somSaksbehandler {
                val behandlingId = opprettBehandlingOgSendTilBeslutter(personIdent)
                assertStatusTotrinnskontroll(behandlingId, TotrinnkontrollStatus.IKKE_AUTORISERT)
                behandlingId
            }
        assertSisteOpprettedeOppgave(behandlingId, Oppgavetype.GodkjenneVedtak)

        somSaksbehandler("annenSaksbehandler") {
            assertStatusTotrinnskontroll(behandlingId, TotrinnkontrollStatus.IKKE_AUTORISERT)
        }

        somBeslutter {
            assertStatusTotrinnskontroll(behandlingId, TotrinnkontrollStatus.KAN_FATTE_VEDTAK)
            godkjennTotrinnskontroll(behandlingId)
            assertStatusTotrinnskontroll(behandlingId, TotrinnkontrollStatus.UAKTUELT)
        }

        verifiserBehandlingIverksettes(behandlingId)
    }

    @Test
    fun `totrinnskontroll skal returnere riktig status etter sendt til beslutter når saksbehandler har beslutterrolle`() {
        val behandlingId =
            somBeslutter {
                val behandlingId = opprettBehandlingOgSendTilBeslutter(personIdent)
                assertStatusTotrinnskontroll(behandlingId, TotrinnkontrollStatus.IKKE_AUTORISERT)
                behandlingId
            }

        somSaksbehandler {
            assertStatusTotrinnskontroll(behandlingId, TotrinnkontrollStatus.IKKE_AUTORISERT)
        }

        somBeslutter("annenBeslutter") {
            assertStatusTotrinnskontroll(behandlingId, TotrinnkontrollStatus.KAN_FATTE_VEDTAK)
        }
    }

    @Test
    fun `underkjenner og sender til beslutter på nytt`() {
        val behandlingId =
            somSaksbehandler {
                val behandlingId = opprettBehandlingOgSendTilBeslutter(personIdent)
                assertSisteFerdigstillOppgaveTask(Oppgavetype.BehandleSak)
                behandlingId
            }

        somBeslutter {
            underkjennTotrinnskontroll(behandlingId)
            assertStatusTotrinnskontroll(behandlingId, TotrinnkontrollStatus.TOTRINNSKONTROLL_UNDERKJENT)
        }

        somSaksbehandler {
            assertStatusTotrinnskontroll(behandlingId, TotrinnkontrollStatus.TOTRINNSKONTROLL_UNDERKJENT)
            sendTilBeslutter(behandlingId)
            assertStatusTotrinnskontroll(behandlingId, TotrinnkontrollStatus.IKKE_AUTORISERT)
            assertSisteFerdigstillOppgaveTask(Oppgavetype.BehandleUnderkjentVedtak)
        }

        somBeslutter {
            godkjennTotrinnskontroll(behandlingId)
            assertStatusTotrinnskontroll(behandlingId, TotrinnkontrollStatus.UAKTUELT)
        }

        verifiserBehandlingIverksettes(behandlingId)
    }

    @Test
    fun `angrer send til beslutter`() {
        val behandlingId =
            somSaksbehandler {
                val behandlingId = opprettBehandlingOgSendTilBeslutter(personIdent)
                assertSisteFerdigstillOppgaveTask(Oppgavetype.BehandleSak)
                behandlingId
            }
        assertSisteOpprettedeOppgave(behandlingId, Oppgavetype.GodkjenneVedtak)

        somSaksbehandler {
            angreSendTilBeslutter(behandlingId)
            assertStatusTotrinnskontroll(behandlingId, TotrinnkontrollStatus.UAKTUELT)
        }

        assertSisteOpprettedeOppgave(behandlingId, Oppgavetype.BehandleSak)

        somSaksbehandler {
            sendTilBeslutter(behandlingId)
            assertStatusTotrinnskontroll(behandlingId, TotrinnkontrollStatus.IKKE_AUTORISERT)
            assertSisteFerdigstillOppgaveTask(Oppgavetype.BehandleSak)
        }

        assertSisteOpprettedeOppgave(behandlingId, Oppgavetype.GodkjenneVedtak)

        somBeslutter {
            godkjennTotrinnskontroll(behandlingId)
            assertStatusTotrinnskontroll(behandlingId, TotrinnkontrollStatus.UAKTUELT)
        }
    }

    @Test
    fun `skal ikke kunne gå videre til vilkår-steg dersom inngangsvilkåren ikke validerer`() {
        somSaksbehandler {
            val behandlingId = opprettBehandling(personIdent)
            vurderInngangsvilkår(behandlingId)
            stegService.resetSteg(behandlingId, StegType.INNGANGSVILKÅR)
            with(vilkårperiodeService.hentVilkårperioder(behandlingId)) {
                val slettVikårperiode = SlettVikårperiode(behandlingId, "kommentar")
                (målgrupper + aktiviteter).forEach {
                    vilkårperiodeService.slettVilkårperiode(it.id, slettVikårperiode)
                }
            }
            assertThatThrownBy {
                stegService.håndterSteg(behandlingId, StegType.INNGANGSVILKÅR)
            }.hasMessageContaining("Finner ingen perioder hvor vilkår for AAP er oppfylt")
        }
    }

    @Test
    fun `skal ikke sende brev vid årsak korrigering uten brev`() {
        val behandlingId =
            somSaksbehandler {
                val behandlingId = opprettBehandling(personIdent)
                val behandling = testoppsettService.hentBehandling(behandlingId)
                testoppsettService.oppdater(behandling.copy(årsak = BehandlingÅrsak.KORRIGERING_UTEN_BREV))

                vurderInngangsvilkår(behandlingId)
                utfyllVilkår(behandlingId)
                opprettVedtak(behandlingId)
                simuler(behandlingId)
                sendTilBeslutter(behandlingId)
                assertSisteFerdigstillOppgaveTask(Oppgavetype.BehandleSak)
                behandlingId
            }
        assertSisteOpprettedeOppgave(behandlingId, Oppgavetype.GodkjenneVedtak)

        somBeslutter {
            godkjennTotrinnskontroll(behandlingId)
        }
        kjørTasks()
        assertStatusTotrinnskontroll(behandlingId, TotrinnkontrollStatus.UAKTUELT)

        with(testoppsettService.hentBehandling(behandlingId)) {
            assertThat(status).isEqualTo(BehandlingStatus.FERDIGSTILT)
            assertThat(steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
        }
    }

    private fun newTransaction() {
        if (TestTransaction.isActive()) {
            TestTransaction.flagForCommit() // need this, otherwise the next line does a rollback
            TestTransaction.end()
            TestTransaction.start()
        }
    }

    private fun angreSendTilBeslutter(behandlingId: BehandlingId) {
        totrinnskontrollController.angreSendTilBeslutter(behandlingId)
        kjørTasks()
    }

    private fun godkjennTotrinnskontroll(behandlingId: BehandlingId) {
        totrinnskontrollController.beslutteVedtak(behandlingId, BeslutteVedtakDto(true))
    }

    private fun underkjennTotrinnskontroll(behandlingId: BehandlingId) {
        totrinnskontrollController.beslutteVedtak(
            behandlingId,
            BeslutteVedtakDto(false, "", listOf(ÅrsakUnderkjent.VEDTAK_OG_BEREGNING)),
        )
        kjørTasks()
    }

    private fun opprettBehandlingOgSendTilBeslutter(personIdent: String): BehandlingId {
        val behandlingId = opprettBehandling(personIdent)
        vurderInngangsvilkår(behandlingId)
        utfyllVilkår(behandlingId)

        opprettVedtak(behandlingId)
        simuler(behandlingId)
        genererSaksbehandlerBrev(behandlingId)
        lagreBrevmottakere(behandlingId)
        sendTilBeslutter(behandlingId)
        return behandlingId
    }

    private fun vurderInngangsvilkår(behandlingId: BehandlingId) {
        val fom = LocalDate.of(2024, 1, 1)
        val tom = LocalDate.of(2024, 1, 31)

        vilkårperiodeService.opprettVilkårperiode(
            dummyVilkårperiodeMålgruppe(
                behandlingId = behandlingId,
                fom = fom,
                tom = tom,
                type = MålgruppeType.AAP,
                dekkesAvAnnetRegelverk = SvarJaNei.NEI,
            ),
        )
        vilkårperiodeService.opprettVilkårperiode(
            dummyVilkårperiodeAktivitet(
                behandlingId = behandlingId,
                fom = fom,
                tom = tom,
                type = AktivitetType.TILTAK,
                svarLønnet = SvarJaNei.NEI,
                aktivitetsdager = 5,
            ),
        )
        stønadsperiodeService.lagreStønadsperioder(
            behandlingId,
            listOf(
                StønadsperiodeDto(
                    fom = fom,
                    tom = tom,
                    målgruppe = MålgruppeType.AAP,
                    aktivitet = AktivitetType.TILTAK,
                    status = StønadsperiodeStatus.NY,
                ),
            ),
        )
        stegService.håndterSteg(behandlingId, StegType.INNGANGSVILKÅR)
    }

    private fun utfyllVilkår(behandlingId: BehandlingId) {
        val barn = barnService.finnBarnPåBehandling(behandlingId).first()
        vilkårController.opprettVilkår(
            OpprettVilkårDto(
                vilkårType = VilkårType.PASS_BARN,
                barnId = barn.id,
                behandlingId = behandlingId,
                delvilkårsett = oppfylteDelvilkårPassBarnDto(),
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 31),
                utgift = 1000,
            ),
        )
        stegService.håndterSteg(behandlingId, StegType.VILKÅR)
    }

    private fun opprettBehandling(personIdent: String): BehandlingId {
        val behandlingId =
            opprettTestBehandlingController.opprettBehandling(
                TestBehandlingRequest(
                    personIdent,
                    stønadstype = Stønadstype.BARNETILSYN,
                ),
            )
        testoppsettService.opprettGrunnlagsdata(behandlingId)
        kjørTasks()
        return behandlingId
    }

    private fun sendTilBeslutter(behandlingId: BehandlingId) {
        kjørTasks()
        totrinnskontrollController.sendTilBeslutter(behandlingId)
        kjørTasks()
    }

    private fun kjørTasks() {
        newTransaction()
        logger.info("Kjører tasks")
        taskService
            .finnAlleTasksKlareForProsessering(Pageable.unpaged())
            .filterNot { it.type == BehandlingsstatistikkTask.TYPE } // Tester ikke statistikkutsendelse her
            .forEach {
                taskWorker.markerPlukket(it.id)
                logger.info("Kjører task ${it.id} type=${it.type} msg=${taskMsg(it)}")
                taskWorker.doActualWork(it.id)
            }
        logger.info("Tasks kjørt OK")
    }

    private fun taskMsg(it: Task): String =
        when (it.type) {
            OpprettOppgaveTask.TYPE ->
                objectMapper
                    .readValue<OpprettOppgaveTask.OpprettOppgaveTaskData>(it.payload)
                    .let { "type=${it.oppgave.oppgavetype} kobling=${it.kobling}" }

            FerdigstillOppgaveTask.TYPE ->
                objectMapper
                    .readValue<FerdigstillOppgaveTask.FerdigstillOppgaveTaskData>(it.payload)
                    .let { "type=${it.oppgavetype} behandling=${it.behandlingId}" }

            else -> it.payload
        }

    private fun verifiserBehandlingIverksettes(behandlingId: BehandlingId) {
        with(testoppsettService.hentBehandling(behandlingId)) {
            assertThat(status).isEqualTo(BehandlingStatus.IVERKSETTER_VEDTAK)
            assertThat(steg).isEqualTo(StegType.JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV)
        }
    }

    private fun assertSisteFerdigstillOppgaveTask(expectedOppgaveType: Oppgavetype) {
        val sisteTask =
            taskService
                .finnTasksMedStatus(Status.entries, FerdigstillOppgaveTask.TYPE)
                .maxByOrNull { it.opprettetTid }!!
        val taskData = objectMapper.readValue<FerdigstillOppgaveTask.FerdigstillOppgaveTaskData>(sisteTask.payload)
        assertThat(taskData.oppgavetype).isEqualTo(expectedOppgaveType)
    }

    private fun assertStatusTotrinnskontroll(
        behandlingId: BehandlingId,
        expectedStatus: TotrinnkontrollStatus,
    ) {
        with(totrinnskontrollService.hentTotrinnskontrollStatus(behandlingId)) {
            assertThat(status).isEqualTo(expectedStatus)
        }
    }

    private fun assertSisteOpprettedeOppgave(
        behandlingId: BehandlingId,
        expectedOppgaveType: Oppgavetype,
    ) {
        assertThat(oppgaveRepository.findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandlingId)?.type)
            .isEqualTo(expectedOppgaveType)
    }

    private fun genererSaksbehandlerBrev(behandlingId: BehandlingId) {
        val html = "SAKSBEHANDLER_SIGNATUR, BREVDATO_PLACEHOLDER, BESLUTTER_SIGNATUR"
        brevController.genererPdf(GenererPdfRequest(html), behandlingId)
    }

    private fun lagreBrevmottakere(behandlingId: BehandlingId) {
        brevmottakereRepository.insert(
            BrevmottakerVedtaksbrev(
                behandlingId = behandlingId,
                mottaker = mottakerPerson(ident = "ident"),
            ),
        )
    }

    private fun opprettVedtak(behandlingId: BehandlingId) {
        tilsynBarnVedtakController.lagreVedtak(
            behandlingId,
            InnvilgelseTilsynBarnRequest,
        )
    }

    private fun simuler(behandlingId: BehandlingId) {
        val saksbehandling = testoppsettService.hentSaksbehandling(behandlingId)
        simuleringStegService.hentEllerOpprettSimuleringsresultat(saksbehandling)
    }

    private fun <T> somSaksbehandler(
        preferredUsername: String = "saksbehandler",
        fn: () -> T,
    ): T {
        kjørTasks()
        return testWithBrukerContext(
            preferredUsername = preferredUsername,
            groups = listOf(rolleConfig.saksbehandlerRolle),
        ) {
            withCallId(fn)
        }
    }

    private fun <T> somBeslutter(
        preferredUsername: String = "beslutter",
        fn: () -> T,
    ): T {
        kjørTasks()
        return testWithBrukerContext(
            preferredUsername = preferredUsername,
            groups = listOf(rolleConfig.beslutterRolle),
        ) {
            withCallId(fn)
        }
    }

    private fun <T> withCallId(fn: () -> T): T {
        MDC.put(MDCConstants.MDC_CALL_ID, IdUtils.generateId())
        val res = fn()
        MDC.remove(MDCConstants.MDC_CALL_ID)
        return res
    }
}
