package no.nav.tilleggsstonader.sak.brev.kjørelistebrev

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository

@Repository
interface KjørelisteBehandlingBrevRepository :
    RepositoryInterface<KjørelisteBehandlingBrev, BehandlingId>,
    InsertUpdateRepository<KjørelisteBehandlingBrev> {
    fun findByBehandlingId(behandlingId: BehandlingId): KjørelisteBehandlingBrev?
}
