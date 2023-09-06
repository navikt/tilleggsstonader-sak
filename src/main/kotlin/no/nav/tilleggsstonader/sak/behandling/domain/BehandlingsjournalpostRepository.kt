package no.nav.tilleggsstonader.sak.behandling.domain

import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BehandlingsjournalpostRepository :
    RepositoryInterface<Behandlingsjournalpost, UUID>, InsertUpdateRepository<Behandlingsjournalpost> {

    fun findAllByBehandlingId(behandlingId: UUID): List<Behandlingsjournalpost>
}

data class Behandlingsjournalpost(
    @Id
    val behandlingId: UUID,
    val journalpostId: String,
    val journalpostType: Journalposttype,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

enum class Journalposttype {
    I,
    U,
    N,
}
