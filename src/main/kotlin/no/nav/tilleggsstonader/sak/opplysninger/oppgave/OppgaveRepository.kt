package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OppgaveRepository : RepositoryInterface<OppgaveDomain, UUID>, InsertUpdateRepository<OppgaveDomain> {

    fun findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId: UUID, oppgavetype: Oppgavetype): OppgaveDomain?

    fun findByType(oppgavetype: Oppgavetype): List<OppgaveDomain>

    fun findByBehandlingIdAndType(
        behandlingId: UUID,
        oppgavetype: Oppgavetype,
    ): List<OppgaveDomain>?

    fun findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(behandlingId: UUID, oppgavetype: Set<Oppgavetype>): OppgaveDomain?

    fun findByGsakOppgaveId(gsakOppgaveId: Long): OppgaveDomain?
    fun findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandlingId: UUID): OppgaveDomain?
    fun findTopByBehandlingIdAndTypeOrderBySporbarOpprettetTidDesc(behandlingId: UUID, type: Oppgavetype): OppgaveDomain?

    @Query(
        """
        SELECT gsak_oppgave_id as first, behandling_id as second FROM oppgave WHERE gsak_oppgave_id IN (:oppgaveIder)
        """,
    )
    fun finnBehandlingIdFor(oppgaveIder: Collection<Long>): List<Pair<Long, UUID>>
}
