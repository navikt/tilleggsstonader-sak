package no.nav.tilleggsstonader.sak.behandling.barn

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository

@Repository
interface BarnRepository :
    RepositoryInterface<BehandlingBarn, BarnId>,
    InsertUpdateRepository<BehandlingBarn> {
    fun findByBehandlingId(behandlingId: BehandlingId): List<BehandlingBarn>
}
