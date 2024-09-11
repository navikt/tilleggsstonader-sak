package no.nav.tilleggsstonader.sak.brev.mellomlager

import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MellomlagerFrittståendeBrevRepository :
    RepositoryInterface<MellomlagretFrittståendeBrev, UUID>,
    InsertUpdateRepository<MellomlagretFrittståendeBrev> {

    fun findByFagsakIdAndSporbarOpprettetAv(fagsakId: FagsakId, sporbarOpprettetAv: String): MellomlagretFrittståendeBrev?
}
