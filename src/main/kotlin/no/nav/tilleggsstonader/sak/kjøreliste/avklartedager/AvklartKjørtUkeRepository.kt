package no.nav.tilleggsstonader.sak.kjøreliste.avklartedager

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AvklartKjørtUkeRepository :
    RepositoryInterface<AvklartKjørtUke, UUID>,
    InsertUpdateRepository<AvklartKjørtUke> {
    fun findByBehandlingId(behandlingId: BehandlingId): List<AvklartKjørtUke>
}
