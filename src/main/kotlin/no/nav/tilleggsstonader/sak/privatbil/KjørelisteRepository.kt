package no.nav.tilleggsstonader.sak.privatbil

import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository

@Repository
interface KjørelisteRepository :
    RepositoryInterface<Kjøreliste, KjørelisteId>,
    InsertUpdateRepository<Kjøreliste> {
    fun findByFagsakId(fagsakId: FagsakId): List<Kjøreliste>

    fun findByJournalpostId(journalpostId: String): Kjøreliste?
}
