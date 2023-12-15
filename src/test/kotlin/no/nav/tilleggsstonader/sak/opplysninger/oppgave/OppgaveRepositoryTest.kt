package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.oppgave
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

internal class OppgaveRepositoryTest : IntegrationTest() {

    @Autowired
    private lateinit var oppgaveRepository: OppgaveRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Test
    fun `skal kunne opprette oppgave uten kobling til behandling`() {
        val oppgave = oppgaveRepository.insert(oppgave(behandlingId = null))

        assertThat(oppgaveRepository.findByIdOrThrow(oppgave.id).behandlingId).isNull()
    }

    @Test
    internal fun findByBehandlingIdAndTypeAndErFerdigstiltIsFalse() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))
        val oppgave = oppgaveRepository.insert(oppgave(behandling, erFerdigstilt = true))

        assertThat(oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(UUID.randomUUID(), Oppgavetype.BehandleSak))
            .isNull()
        assertThat(oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandling.id, Oppgavetype.BehandleSak))
            .isNull()
        assertThat(oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandling.id, oppgave.type))
            .isNull()

        val oppgaveIkkeFerdigstilt = oppgaveRepository.insert(oppgave(behandling))
        assertThat(oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandling.id, oppgave.type))
            .isEqualTo(oppgaveIkkeFerdigstilt)
    }

    @Test
    internal fun findByBehandlingIdAndTypeInAndErFerdigstiltIsFalse() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))
        oppgaveRepository.insert(oppgave(behandling, erFerdigstilt = false, type = Oppgavetype.Journalføring))
        oppgaveRepository.insert(oppgave(behandling, erFerdigstilt = true, type = Oppgavetype.BehandleSak))
        oppgaveRepository.insert(oppgave(behandling, erFerdigstilt = false, type = Oppgavetype.BehandleUnderkjentVedtak))

        val oppgave =
            oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
                behandling.id,
                setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak),
            )
        assertThat(oppgave).isNotNull
        assertThat(oppgave?.type).isEqualTo(Oppgavetype.BehandleUnderkjentVedtak)
    }

    @Test
    internal fun `skal finne nyeste oppgave for behandling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))
        val sporbar = Sporbar(opprettetTid = LocalDateTime.now().plusDays(1))
        oppgaveRepository.insert(oppgave(behandling, erFerdigstilt = true, gsakOppgaveId = 1))
        oppgaveRepository.insert(oppgave(behandling, erFerdigstilt = true, gsakOppgaveId = 2).copy(sporbar = sporbar))
        oppgaveRepository.insert(oppgave(behandling, erFerdigstilt = true, gsakOppgaveId = 3))
        oppgaveRepository.insert(oppgave(behandling, erFerdigstilt = true, gsakOppgaveId = 4))

        assertThat(oppgaveRepository.findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandling.id)).isNotNull
        assertThat(oppgaveRepository.findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandling.id)?.gsakOppgaveId)
            .isEqualTo(2)
    }

    @Test
    internal fun `skal finne nyeste oppgave for riktig behandling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        val behandling2 = testoppsettService.lagre(behandling(fagsak))

        oppgaveRepository.insert(oppgave(behandling, erFerdigstilt = true, gsakOppgaveId = 1))
        oppgaveRepository.insert(oppgave(behandling2, erFerdigstilt = true, gsakOppgaveId = 2))

        assertThat(oppgaveRepository.findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandling.id)).isNotNull()
        assertThat(oppgaveRepository.findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandling.id)?.gsakOppgaveId)
            .isEqualTo(1)
    }

    @Test
    internal fun `skal finne oppgaver for oppgavetype og personident`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        oppgaveRepository.insert(
            OppgaveDomain(
                behandlingId = behandling.id,
                type = Oppgavetype.InnhentDokumentasjon,
                gsakOppgaveId = 1,
            ),
        )
        oppgaveRepository.insert(
            OppgaveDomain(
                behandlingId = behandling.id,
                type = Oppgavetype.BehandleSak,
                gsakOppgaveId = 1,
            ),
        )

        assertThat(
            oppgaveRepository.findByType(
                Oppgavetype.InnhentDokumentasjon,
            ).size,
        ).isEqualTo(1)
    }

    @Test
    internal fun `skal ikke feile hvis det ikke finnes en oppgave for behandlingen`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))

        assertThat(oppgaveRepository.findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandling.id)).isNull()
    }
}
