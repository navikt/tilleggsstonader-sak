package no.nav.tilleggsstonader.sak.brev.brevmottaker

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BrevmottakerRepository : RepositoryInterface<Brevmottaker, UUID>, InsertUpdateRepository<Brevmottaker> {

    fun existsByBehandlingId(behandlingId: UUID): Boolean
    fun findByBehandlingId(behandlingId: UUID): List<Brevmottaker>
}
