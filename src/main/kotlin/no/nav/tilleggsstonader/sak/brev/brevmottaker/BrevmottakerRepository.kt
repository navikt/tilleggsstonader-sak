package no.nav.tilleggsstonader.sak.brev.brevmottaker

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BrevmottakerRepository : RepositoryInterface<Brevmottaker, UUID>, InsertUpdateRepository<Brevmottaker> {

    fun existsByBehandlingId(behandlingId: BehandlingId): Boolean
    fun findByBehandlingId(behandlingId: BehandlingId): List<Brevmottaker>
}
