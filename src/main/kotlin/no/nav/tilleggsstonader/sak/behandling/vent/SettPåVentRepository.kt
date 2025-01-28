package no.nav.tilleggsstonader.sak.behandling.vent

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SettPåVentRepository :
    RepositoryInterface<SettPåVent, UUID>,
    InsertUpdateRepository<SettPåVent> {
    fun findByBehandlingIdAndAktivIsTrue(behandlingId: BehandlingId): SettPåVent?
}
