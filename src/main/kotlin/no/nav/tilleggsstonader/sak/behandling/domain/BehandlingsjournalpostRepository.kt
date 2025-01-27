package no.nav.tilleggsstonader.sak.behandling.domain

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.stereotype.Repository

@Repository
interface BehandlingsjournalpostRepository :
    RepositoryInterface<Behandlingsjournalpost, BehandlingId>,
    InsertUpdateRepository<Behandlingsjournalpost> {
    fun findAllByBehandlingId(behandlingId: BehandlingId): List<Behandlingsjournalpost>
}

data class Behandlingsjournalpost(
    @Id
    val behandlingId: BehandlingId,
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
