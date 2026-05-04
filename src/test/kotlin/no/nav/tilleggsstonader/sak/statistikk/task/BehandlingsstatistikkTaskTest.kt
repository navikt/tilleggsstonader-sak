package no.nav.tilleggsstonader.sak.statistikk.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.prosessering.error.RekjørSenereException
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingMetode
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.statistikk.behandling.BehandlingsstatistikkService
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class BehandlingsstatistikkTaskTest {
    private val behandlingsstatistikkService = mockk<BehandlingsstatistikkService>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>()
    private val oppgaveService = mockk<OppgaveService>()

    private val taskStep = BehandlingsstatistikkTask(behandlingsstatistikkService, behandlingService, oppgaveService)

    @Test
    fun `skal rekjøre for kjøreliste i opprettet status uten oppgave`() {
        val saksbehandling =
            saksbehandling(
                type = BehandlingType.KJØRELISTE,
                status = BehandlingStatus.OPPRETTET,
            )
        val task = BehandlingsstatistikkTask.opprettMottattTask(saksbehandling.id, saksbehandling.opprettetTid)

        every { behandlingService.hentSaksbehandling(saksbehandling.id) } returns saksbehandling
        every { oppgaveService.finnAlleOppgaveDomainForBehandling(saksbehandling.id) } returns emptyList()

        assertThatThrownBy { taskStep.doTask(task) }
            .isInstanceOf(RekjørSenereException::class.java)
    }

    @Test
    fun `skal sende statistikk for automatisk behandling uten oppgave`() {
        val saksbehandling =
            saksbehandling(
                type = BehandlingType.KJØRELISTE,
                status = BehandlingStatus.FERDIGSTILT,
                behandlingMetode = BehandlingMetode.AUTOMATISK,
            )
        val task = BehandlingsstatistikkTask.opprettMottattTask(saksbehandling.id, saksbehandling.opprettetTid)

        every { behandlingService.hentSaksbehandling(saksbehandling.id) } returns saksbehandling
        every { oppgaveService.finnAlleOppgaveDomainForBehandling(saksbehandling.id) } returns emptyList()

        assertThatCode { taskStep.doTask(task) }.doesNotThrowAnyException()

        verify(exactly = 1) {
            behandlingsstatistikkService.sendBehandlingstatistikk(
                saksbehandling = saksbehandling,
                hendelse = any(),
                hendelseTidspunkt = any(),
                gjeldendeSaksbehandler = any(),
            )
        }
    }
}
