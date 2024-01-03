package no.nav.tilleggsstonader.sak.behandlingsflyt

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
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveDomain
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveRepository
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.InnvilgelseTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.Stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnVedtakController
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.Utgift
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollController
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.TotrinnkontrollStatus
import no.nav.tilleggsstonader.sak.vilkår.VilkårController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.random.Random

class BehandlingFlytTest(
    @Autowired val barnService: BarnService,
    @Autowired val oppgaveRepository: OppgaveRepository,
    @Autowired val opprettTestBehandlingController: OpprettTestBehandlingController,
    @Autowired val testSaksbehandlingController: TestSaksbehandlingController,
    @Autowired val vilkårController: VilkårController,
    @Autowired val tilsynBarnVedtakController: TilsynBarnVedtakController,
    @Autowired val brevController: BrevController,
    @Autowired val totrinnskontrollController: TotrinnskontrollController,
) : IntegrationTest() {

    @Test
    fun `saksbehandler innvilger vedtak og beslutter godkjenner vedtak`() {
        val personIdent = FnrGenerator.generer(2000)

        val behandlingId = saksbehandler {
            val behandlingId = opprettTestBehandlingController.opprettBehandling(TestBehandlingRequest(personIdent))
            opprettOppgave(behandlingId)
            vilkårController.getVilkår(behandlingId)
            testSaksbehandlingController.utfyllVilkår(behandlingId)

            opprettVedtak(behandlingId)
            genererSaksbehandlerBrev(behandlingId)
            totrinnskontrollController.sendTilBeslutter(behandlingId)
            assertStatusTotrinnskontroll(behandlingId, TotrinnkontrollStatus.IKKE_AUTORISERT)
            behandlingId
        }

        beslutter {
            assertStatusTotrinnskontroll(behandlingId, TotrinnkontrollStatus.KAN_FATTE_VEDTAK)
            totrinnskontrollController.beslutteVedtak(behandlingId, BeslutteVedtakDto(true))
            assertStatusTotrinnskontroll(behandlingId, TotrinnkontrollStatus.UAKTUELT)
        }

        verifiserBehandlingIverksettes(behandlingId)
    }

    private fun verifiserBehandlingIverksettes(behandlingId: UUID) {
        with(testoppsettService.hentBehandling(behandlingId)) {
            assertThat(status).isEqualTo(BehandlingStatus.IVERKSETTER_VEDTAK)
            assertThat(steg).isEqualTo(StegType.VENTE_PÅ_STATUS_FRA_UTBETALING)
        }
    }

    private fun assertStatusTotrinnskontroll(behandlingId: UUID, expectedStatus: TotrinnkontrollStatus) {
        with(totrinnskontrollController.hentTotrinnskontroll(behandlingId)) {
            assertThat(status).isEqualTo(expectedStatus)
        }
    }

    private fun opprettOppgave(behandlingId: UUID, type: Oppgavetype = Oppgavetype.BehandleSak) {
        val oppgaveId = Random.nextInt(100, 200).toLong()
        val oppgave = OppgaveDomain(
            gsakOppgaveId = oppgaveId,
            behandlingId = behandlingId,
            type = type,
        )
        oppgaveRepository.insert(oppgave)
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
            behandlingId, InnvilgelseTilsynBarnDto(stønadsperioder, utgifter)
        )
    }

    private fun stønadsperiode() = Stønadsperiode(
        fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 31)
    )

    private fun utgift() = Utgift(
        fom = YearMonth.of(2023, 1), tom = YearMonth.of(2023, 1), utgift = 1000
    )

    private fun <T> saksbehandler(fn: () -> T) =
        testWithBrukerContext(preferredUsername = "saksbehandler", groups = listOf(rolleConfig.saksbehandlerRolle)) {
            withCallId(fn)
        }

    private fun <T> beslutter(fn: () -> T) =
        testWithBrukerContext(preferredUsername = "beslutter", groups = listOf(rolleConfig.beslutterRolle)) {
            withCallId(fn)
        }

    private fun <T> withCallId(fn: () -> T): T {
        MDC.put(MDCConstants.MDC_CALL_ID, IdUtils.generateId())
        val res = fn()
        MDC.remove(MDCConstants.MDC_CALL_ID)
        return res
    }
}