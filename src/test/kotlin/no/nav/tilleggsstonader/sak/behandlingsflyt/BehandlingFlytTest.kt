package no.nav.tilleggsstonader.sak.behandlingsflyt

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.prosessering.internal.TaskWorker
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.libs.log.IdUtils
import no.nav.tilleggsstonader.libs.log.mdc.MDCConstants
import no.nav.tilleggsstonader.libs.test.fnr.FnrGenerator
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.OpprettTestBehandlingController
import no.nav.tilleggsstonader.sak.behandling.TestBehandlingRequest
import no.nav.tilleggsstonader.sak.behandling.TestSaksbehandlingController
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.brev.BrevController
import no.nav.tilleggsstonader.sak.brev.GenererPdfRequest
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveRepository
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.FerdigstillOppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.pdl.logger
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.InnvilgelseTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.Stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnVedtakController
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.Utgift
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollController
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.TotrinnkontrollStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.ÅrsakUnderkjent
import no.nav.tilleggsstonader.sak.vilkår.VilkårController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.test.context.transaction.TestTransaction
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class BehandlingFlytTest(
    @Autowired val barnService: BarnService,
    @Autowired val oppgaveRepository: OppgaveRepository,
    @Autowired val opprettTestBehandlingController: OpprettTestBehandlingController,
    @Autowired val testSaksbehandlingController: TestSaksbehandlingController,
    @Autowired val vilkårController: VilkårController,
    @Autowired val tilsynBarnVedtakController: TilsynBarnVedtakController,
    @Autowired val brevController: BrevController,
    @Autowired val totrinnskontrollController: TotrinnskontrollController,
    @Autowired val taskService: TaskService,
    @Autowired val taskWorker: TaskWorker,
) : IntegrationTest() {

    val personIdent = FnrGenerator.generer(år = 2000)

    @Test
    fun `saksbehandler innvilger vedtak og beslutter godkjenner vedtak`() {
        val behandlingId = somSaksbehandler {
            val behandlingId = opprettBehandlingOgSendTilBeslutter(personIdent)
            assertStatusTotrinnskontroll(behandlingId, TotrinnkontrollStatus.IKKE_AUTORISERT)
            behandlingId
        }
        assertSisteOpprettedeOppgave(behandlingId, Oppgavetype.GodkjenneVedtak)

        somBeslutter {
            assertStatusTotrinnskontroll(behandlingId, TotrinnkontrollStatus.KAN_FATTE_VEDTAK)
            godkjennTotrinnskontroll(behandlingId)
            assertStatusTotrinnskontroll(behandlingId, TotrinnkontrollStatus.UAKTUELT)
        }

        verifiserBehandlingIverksettes(behandlingId)
    }

    @Test
    fun `underkjenner og sender til beslutter på nytt`() {
        val behandlingId = somSaksbehandler {
            val behandlingId = opprettBehandlingOgSendTilBeslutter(personIdent)
            assertStatusTotrinnskontroll(behandlingId, TotrinnkontrollStatus.IKKE_AUTORISERT)
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
        val behandlingId = somSaksbehandler {
            val behandlingId = opprettBehandlingOgSendTilBeslutter(personIdent)
            assertStatusTotrinnskontroll(behandlingId, TotrinnkontrollStatus.IKKE_AUTORISERT)
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

    private fun newTransaction() {
        if (TestTransaction.isActive()) {
            TestTransaction.flagForCommit(); // need this, otherwise the next line does a rollback
            TestTransaction.end()
            TestTransaction.start()
        }
    }

    private fun angreSendTilBeslutter(behandlingId: UUID) {
        totrinnskontrollController.angreSendTilBeslutter(behandlingId)
        kjørTasks()
    }

    private fun godkjennTotrinnskontroll(behandlingId: UUID) {
        totrinnskontrollController.beslutteVedtak(behandlingId, BeslutteVedtakDto(true))
        kjørTasks()
    }

    private fun underkjennTotrinnskontroll(behandlingId: UUID) {
        totrinnskontrollController.beslutteVedtak(
            behandlingId,
            BeslutteVedtakDto(false, "", listOf(ÅrsakUnderkjent.VEDTAK_OG_BEREGNING)),
        )
        kjørTasks()
    }

    private fun opprettBehandlingOgSendTilBeslutter(personIdent: String): UUID {
        val behandlingId = opprettBehandling(personIdent)
        vilkårController.getVilkår(behandlingId)
        testSaksbehandlingController.utfyllVilkår(behandlingId)

        opprettVedtak(behandlingId)
        genererSaksbehandlerBrev(behandlingId)
        sendTilBeslutter(behandlingId)
        return behandlingId
    }

    private fun opprettBehandling(personIdent: String): UUID {
        val behandlingId = opprettTestBehandlingController.opprettBehandling(TestBehandlingRequest(personIdent))
        kjørTasks()
        return behandlingId
    }

    private fun sendTilBeslutter(behandlingId: UUID) {
        kjørTasks()
        totrinnskontrollController.sendTilBeslutter(behandlingId)
        kjørTasks()
    }

    private fun kjørTasks() {
        newTransaction()
        logger.info("Kjører tasks")
        taskService.finnAlleTasksKlareForProsessering(Pageable.unpaged()).forEach {
            taskWorker.markerPlukket(it.id)
            logger.info("Kjører task ${it.id} type=${it.type} payload=${it.payload}")
            taskWorker.doActualWork(it.id)
        }
        logger.info("Tasks kjørt OK")
    }

    private fun verifiserBehandlingIverksettes(behandlingId: UUID) {
        with(testoppsettService.hentBehandling(behandlingId)) {
            assertThat(status).isEqualTo(BehandlingStatus.IVERKSETTER_VEDTAK)
            assertThat(steg).isEqualTo(StegType.VENTE_PÅ_STATUS_FRA_UTBETALING)
        }
    }

    private fun assertSisteFerdigstillOppgaveTask(expectedOppgaveType: Oppgavetype) {
        val sisteTask = taskService.finnTasksMedStatus(Status.entries, FerdigstillOppgaveTask.TYPE)
            .maxByOrNull { it.opprettetTid }!!
        val taskData = objectMapper.readValue<FerdigstillOppgaveTask.FerdigstillOppgaveTaskData>(sisteTask.payload)
        assertThat(taskData.oppgavetype).isEqualTo(expectedOppgaveType)
    }

    private fun assertStatusTotrinnskontroll(behandlingId: UUID, expectedStatus: TotrinnkontrollStatus) {
        with(totrinnskontrollController.hentTotrinnskontroll(behandlingId)) {
            assertThat(status).isEqualTo(expectedStatus)
        }
    }

    private fun assertSisteOpprettedeOppgave(behandlingId: UUID, expectedOppgaveType: Oppgavetype) {
        assertThat(oppgaveRepository.findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandlingId)?.type)
            .isEqualTo(expectedOppgaveType)
    }

    private fun genererSaksbehandlerBrev(behandlingId: UUID) {
        val html = "SAKSBEHANDLER_SIGNATUR, BREVDATO_PLACEHOLDER, BESLUTTER_SIGNATUR"
        brevController.genererPdf(GenererPdfRequest(html), behandlingId)
    }

    private fun opprettVedtak(behandlingId: UUID) {
        val barn = barnService.finnBarnPåBehandling(behandlingId)
        val utgifter = barn.first().let { mapOf(it.id to listOf(utgift())) }
        val stønadsperioder = listOf(stønadsperiode())
        tilsynBarnVedtakController.lagreVedtak(
            behandlingId,
            InnvilgelseTilsynBarnDto(stønadsperioder, utgifter),
        )
    }

    private fun stønadsperiode() = Stønadsperiode(
        fom = LocalDate.of(2023, 1, 1),
        tom = LocalDate.of(2023, 1, 31),
    )

    private fun utgift() = Utgift(
        fom = YearMonth.of(2023, 1),
        tom = YearMonth.of(2023, 1),
        utgift = 1000,
    )

    private fun <T> somSaksbehandler(fn: () -> T): T {
        kjørTasks()
        return testWithBrukerContext(
            preferredUsername = "saksbehandler",
            groups = listOf(rolleConfig.saksbehandlerRolle),
        ) {
            withCallId(fn)
        }
    }

    private fun <T> somBeslutter(fn: () -> T): T {
        kjørTasks()
        return testWithBrukerContext(preferredUsername = "beslutter", groups = listOf(rolleConfig.beslutterRolle)) {
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
