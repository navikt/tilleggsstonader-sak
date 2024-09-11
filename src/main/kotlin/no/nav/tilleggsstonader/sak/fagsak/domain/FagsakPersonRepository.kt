package no.nav.tilleggsstonader.sak.fagsak.domain

import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository

@Repository
interface FagsakPersonRepository : RepositoryInterface<FagsakPerson, FagsakPersonId>, InsertUpdateRepository<FagsakPerson> {

    @Query(
        """SELECT p.* FROM fagsak_person p WHERE 
                EXISTS(SELECT 1 FROM person_ident WHERE fagsak_person_id = p.id AND ident IN (:identer))""",
    )
    fun findByIdent(identer: Collection<String>): FagsakPerson?

    @Query("SELECT * FROM person_ident WHERE fagsak_person_id = :personId")
    fun findPersonIdenter(personId: FagsakPersonId): Set<PersonIdent>

    @Query("SELECT ident FROM person_ident WHERE fagsak_person_id = :personId ORDER BY endret_tid DESC LIMIT 1")
    fun hentAktivIdent(personId: FagsakPersonId): String
}
