package no.nav.tilleggsstonader.sak.brev

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JournalpostResultatRepository : RepositoryInterface<JournalpostResultat, UUID>, InsertUpdateRepository<JournalpostResultat> {

    fun findByBehandlingIdAndMottakerId(behandlingId: UUID, mottakerId: String): JournalpostResultat
}
