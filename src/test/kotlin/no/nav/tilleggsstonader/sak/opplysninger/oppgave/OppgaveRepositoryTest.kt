package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.oppgave
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnInternStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnskontrollRepository
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnskontrollUtil.totrinnskontroll
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

internal class OppgaveRepositoryTest : IntegrationTest() {
    @Autowired
    private lateinit var oppgaveRepository: OppgaveRepository

    @Autowired
    private lateinit var totrinnskontrollRepository: TotrinnskontrollRepository

    @Test
    fun `skal kunne opprette oppgave uten kobling til behandling`() {
        val oppgave = oppgaveRepository.insert(oppgave(behandlingId = null))

        assertThat(oppgaveRepository.findByIdOrThrow(oppgave.id).behandlingId).isNull()
    }

    @Test
    internal fun findByBehandlingIdAndTypeAndStatus() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))
        val oppgave = oppgaveRepository.insert(oppgave(behandling, status = Oppgavestatus.FERDIGSTILT))

        assertThat(
            oppgaveRepository.findByBehandlingIdAndTypeAndStatus(
                BehandlingId.random(),
                Oppgavetype.BehandleSak,
                Oppgavestatus.ÅPEN,
            ),
        ).isNull()
        assertThat(
            oppgaveRepository.findByBehandlingIdAndTypeAndStatus(
                behandling.id,
                Oppgavetype.BehandleSak,
                Oppgavestatus.ÅPEN,
            ),
        ).isNull()
        assertThat(oppgaveRepository.findByBehandlingIdAndTypeAndStatus(behandling.id, oppgave.type, Oppgavestatus.ÅPEN))
            .isNull()

        val oppgaveIkkeFerdigstilt = oppgaveRepository.insert(oppgave(behandling))
        assertThat(oppgaveRepository.findByBehandlingIdAndTypeAndStatus(behandling.id, oppgave.type, Oppgavestatus.ÅPEN))
            .isEqualTo(oppgaveIkkeFerdigstilt)
    }

    @Test
    internal fun findByBehandlingIdAndTypeInAndErFerdigstiltIsFalse() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))
        oppgaveRepository.insert(oppgave(behandling, type = Oppgavetype.Journalføring))
        oppgaveRepository.insert(oppgave(behandling, status = Oppgavestatus.FERDIGSTILT, type = Oppgavetype.BehandleSak))
        oppgaveRepository.insert(
            oppgave(
                behandling = behandling,
                status = Oppgavestatus.ÅPEN,
                type = Oppgavetype.BehandleUnderkjentVedtak,
            ),
        )

        val oppgave =
            oppgaveRepository.findByBehandlingIdAndStatusAndTypeIn(
                behandlingId = behandling.id,
                status = Oppgavestatus.ÅPEN,
                oppgavetype = setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak),
            )
        assertThat(oppgave).isNotNull
        assertThat(oppgave?.type).isEqualTo(Oppgavetype.BehandleUnderkjentVedtak)
    }

    @Test
    internal fun `skal finne nyeste oppgave for behandling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))
        val sporbar = Sporbar(opprettetTid = LocalDateTime.now().plusDays(1))
        oppgaveRepository.insert(oppgave(behandling, status = Oppgavestatus.FERDIGSTILT, gsakOppgaveId = 1))
        oppgaveRepository.insert(oppgave(behandling, status = Oppgavestatus.FERDIGSTILT, gsakOppgaveId = 2).copy(sporbar = sporbar))
        oppgaveRepository.insert(oppgave(behandling, status = Oppgavestatus.FERDIGSTILT, gsakOppgaveId = 3))
        oppgaveRepository.insert(oppgave(behandling, status = Oppgavestatus.FERDIGSTILT, gsakOppgaveId = 4))

        assertThat(oppgaveRepository.findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandling.id)).isNotNull
        assertThat(oppgaveRepository.findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandling.id)?.gsakOppgaveId)
            .isEqualTo(2)
    }

    @Test
    internal fun `skal finne nyeste oppgave for riktig behandling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        val behandling2 = testoppsettService.lagre(behandling(fagsak))

        oppgaveRepository.insert(oppgave(behandling, status = Oppgavestatus.FERDIGSTILT, gsakOppgaveId = 1))
        oppgaveRepository.insert(oppgave(behandling2, status = Oppgavestatus.FERDIGSTILT, gsakOppgaveId = 2))

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
                tilordnetSaksbehandler = null,
                status = Oppgavestatus.ÅPEN,
                tildeltEnhetsnummer = null,
                enhetsmappeId = null,
            ),
        )
        oppgaveRepository.insert(
            OppgaveDomain(
                behandlingId = behandling.id,
                type = Oppgavetype.BehandleSak,
                gsakOppgaveId = 1,
                tilordnetSaksbehandler = null,
                status = Oppgavestatus.ÅPEN,
                tildeltEnhetsnummer = null,
                enhetsmappeId = null,
            ),
        )

        assertThat(
            oppgaveRepository
                .findByType(
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

    @Nested
    inner class FinnOppgaveMetadata {
        val fagsak1 = fagsak()
        val fagsak2 = fagsak(identer = setOf(PersonIdent("2")))
        val behandling1 = behandling(fagsak1)
        val behandling2 = behandling(fagsak2)

        @BeforeEach
        fun setUp() {
            testoppsettService.lagreFagsak(fagsak1)
            testoppsettService.lagreFagsak(fagsak2)
            testoppsettService.lagre(behandling1)
            testoppsettService.lagre(behandling2)
        }

        @Test
        fun `skal ikke hente ut informasjon til de oppgaver som ikke har behandlingId`() {
            oppgaveRepository.insert(
                OppgaveDomain(
                    behandlingId = null,
                    type = Oppgavetype.Journalføring,
                    gsakOppgaveId = 1,
                    tilordnetSaksbehandler = null,
                    status = Oppgavestatus.ÅPEN,
                    tildeltEnhetsnummer = null,
                    enhetsmappeId = null,
                ),
            )

            assertThat(oppgaveRepository.finnOppgaveMetadata(listOf(1))).isEmpty()
        }

        @Test
        fun `skal finne behandlingId til oppgaver`() {
            opprettOppgaver()
            val metadata = oppgaveRepository.finnOppgaveMetadata(listOf(1, 2, 3))

            assertThat(metadata.map { it.gsakOppgaveId to it.behandlingId })
                .containsExactlyInAnyOrder(Pair(1, behandling1.id.id), Pair(2, behandling2.id.id))
        }

        @Test
        fun `skal finne hvem som sendt oppgaven til totrinnskontroll`() {
            opprettOppgaver()
            totrinnskontrollRepository.insert(
                totrinnskontroll(
                    status = TotrinnInternStatus.KAN_FATTE_VEDTAK,
                    behandlingId = behandling1.id,
                    saksbehandler = "sak001",
                ),
            )

            val metadata = oppgaveRepository.finnOppgaveMetadata(listOf(1, 2, 3))

            assertThat(metadata.map { it.gsakOppgaveId to it.sendtTilTotrinnskontrollAv })
                .containsExactlyInAnyOrder(Pair(1, "sak001"), Pair(2, null))
        }

        @Test
        fun `sendtTilTotrinnskontrollAv er null hvis TotrinnInternStatus ikke er KAN_FATTE_VEDTAK`() {
            opprettOppgaver()

            TotrinnInternStatus.entries
                .filter { it != TotrinnInternStatus.KAN_FATTE_VEDTAK }
                .forEach {
                    totrinnskontrollRepository.insert(
                        totrinnskontroll(
                            status = it,
                            behandlingId = behandling1.id,
                            saksbehandler = "sak002",
                        ),
                    )
                }
            val metadata = oppgaveRepository.finnOppgaveMetadata(listOf(1, 2, 3))

            assertThat(metadata.map { it.gsakOppgaveId to it.sendtTilTotrinnskontrollAv })
                .containsExactlyInAnyOrder(Pair(1, null), Pair(2, null))
        }

        private fun opprettOppgaver() {
            oppgaveRepository.insert(
                OppgaveDomain(
                    behandlingId = behandling1.id,
                    type = Oppgavetype.BehandleSak,
                    gsakOppgaveId = 1,
                    tilordnetSaksbehandler = null,
                    status = Oppgavestatus.ÅPEN,
                    tildeltEnhetsnummer = null,
                    enhetsmappeId = null,
                ),
            )
            oppgaveRepository.insert(
                OppgaveDomain(
                    behandlingId = behandling2.id,
                    type = Oppgavetype.BehandleSak,
                    gsakOppgaveId = 2,
                    tilordnetSaksbehandler = null,
                    status = Oppgavestatus.ÅPEN,
                    tildeltEnhetsnummer = null,
                    enhetsmappeId = null,
                ),
            )
        }
    }
}
