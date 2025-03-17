package no.nav.tilleggsstonader.sak.behandling.barn

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository

@Repository
interface BarnRepository :
    RepositoryInterface<BehandlingBarn, BarnId>,
    InsertUpdateRepository<BehandlingBarn> {
    fun findByBehandlingId(behandlingId: BehandlingId): List<BehandlingBarn>

    @Query(
        """
        SELECT bb.ident AS ident
        FROM behandling_barn bb
        JOIN behandling b ON bb.behandling_id = b.id
        JOIN fagsak f ON b.fagsak_id = f.id
        WHERE f.fagsak_person_id = :fagsakPersonId
    """,
    )
    fun finnIdenterTilFagsakPersonId(fagsakPersonId: FagsakPersonId): Set<String>
}
