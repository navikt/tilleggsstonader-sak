package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OppgaveRepository : RepositoryInterface<OppgaveDomain, UUID>, InsertUpdateRepository<OppgaveDomain> {

    fun findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId: BehandlingId, oppgavetype: Oppgavetype): OppgaveDomain?

    fun findByType(oppgavetype: Oppgavetype): List<OppgaveDomain>

    fun findByBehandlingIdAndType(
        behandlingId: BehandlingId,
        oppgavetype: Oppgavetype,
    ): List<OppgaveDomain>?

    fun findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(behandlingId: BehandlingId, oppgavetype: Set<Oppgavetype>): OppgaveDomain?

    fun findByGsakOppgaveId(gsakOppgaveId: Long): OppgaveDomain?
    fun findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandlingId: BehandlingId): OppgaveDomain?
    fun findTopByBehandlingIdAndTypeOrderBySporbarOpprettetTidDesc(behandlingId: BehandlingId, type: Oppgavetype): OppgaveDomain?

    @Query(
        """
        SELECT gsak_oppgave_id, o.behandling_id, 
        t.saksbehandler AS sendt_til_totrinnskontroll_av,
        CASE 
            WHEN v.type = 'OPPHØR' THEN TRUE 
            ELSE FALSE 
        END AS er_opphor
        FROM oppgave o
        LEFT JOIN totrinnskontroll t ON t.behandling_id = o.behandling_id AND t.status = 'KAN_FATTE_VEDTAK'
        LEFT JOIN vedtak v ON o.behandling_id = v.behandling_id 
        WHERE gsak_oppgave_id IN (:oppgaveIder)
        AND o.behandling_id IS NOT NULL
        """,
    )
    fun finnOppgaveMetadata(oppgaveIder: Collection<Long>): List<OppgaveMetadata>
}
