package no.nav.tilleggsstonader.sak.behandling.domain

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface EksternBehandlingIdRepository :
    RepositoryInterface<EksternBehandlingId, Long>,
    InsertUpdateRepository<EksternBehandlingId> {

    fun findByBehandlingId(behandlingId: UUID): EksternBehandlingId
}
