package no.nav.tilleggsstonader.sak.brev.brevmottaker

import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerFrittståendeBrev
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BrevmottakerFrittståendeBrevRepository :
    RepositoryInterface<BrevmottakerFrittståendeBrev, UUID>,
    InsertUpdateRepository<BrevmottakerFrittståendeBrev> {
    fun existsByFagsakIdAndSporbarOpprettetAvAndBrevIdIsNull(
        fagsakId: FagsakId,
        opprettetAv: String,
    ): Boolean

    fun findByFagsakIdAndSporbarOpprettetAvAndBrevIdIsNull(
        fagsakId: FagsakId,
        opprettetAv: String,
    ): List<BrevmottakerFrittståendeBrev>
}
