package no.nav.tilleggsstonader.sak.behandling.domain

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository

@Repository
interface EksternBehandlingIdRepository :
    RepositoryInterface<EksternBehandlingId, Long>,
    InsertUpdateRepository<EksternBehandlingId> {
    fun findByBehandlingId(behandlingId: BehandlingId): EksternBehandlingId
}
