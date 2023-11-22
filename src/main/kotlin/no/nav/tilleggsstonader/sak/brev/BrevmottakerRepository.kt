package no.nav.tilleggsstonader.sak.brev

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BrevmottakerRepository : RepositoryInterface<Brevmottaker, UUID>, InsertUpdateRepository<Brevmottaker> {

}
