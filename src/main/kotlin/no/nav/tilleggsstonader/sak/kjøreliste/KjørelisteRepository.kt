package no.nav.tilleggsstonader.sak.kjøreliste

import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface KjørelisteRepository :
    RepositoryInterface<Kjøreliste, UUID>,
    InsertUpdateRepository<Kjøreliste> {
    fun findByFagsakId(fagsakId: FagsakId): List<Kjøreliste>

    fun findByJournalpostId(journalpostId: String): Kjøreliste?
}
