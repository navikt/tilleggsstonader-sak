package no.nav.tilleggsstonader.sak.fagsak.domain

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface EksternFagsakIdRepository : RepositoryInterface<EksternFagsakId, Long>, InsertUpdateRepository<EksternFagsakId> {

    fun findByFagsakId(fagsakId: UUID): EksternFagsakId
}
