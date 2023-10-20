package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OppgaveRepository : RepositoryInterface<TSOppgave, Long>, InsertUpdateRepository<TSOppgave> {

    fun findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId: UUID, oppgavetype: Oppgavetype): TSOppgave?

    fun findByBehandlingIdAndType(
        behandlingId: UUID,
        oppgavetype: Oppgavetype,
    ): List<TSOppgave>?

    fun findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(behandlingId: UUID, oppgavetype: Set<Oppgavetype>): TSOppgave?

    fun findByGsakOppgaveId(gsakOppgaveId: Long): TSOppgave?
    fun findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandlingId: UUID): TSOppgave?
    fun findTopByBehandlingIdAndTypeOrderBySporbarOpprettetTidDesc(behandlingId: UUID, type: Oppgavetype): TSOppgave?
}
