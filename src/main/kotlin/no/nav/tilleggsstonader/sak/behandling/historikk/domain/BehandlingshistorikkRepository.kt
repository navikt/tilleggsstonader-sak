package no.nav.tilleggsstonader.sak.behandling.historikk.domain

import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BehandlingshistorikkRepository :
    RepositoryInterface<Behandlingshistorikk, UUID>,
    InsertUpdateRepository<Behandlingshistorikk> {

    fun findByBehandlingIdOrderByEndretTidDesc(behandlingId: BehandlingId): List<Behandlingshistorikk>

    fun findByBehandlingIdOrderByEndretTidAsc(behandlingId: BehandlingId): List<Behandlingshistorikk>

    fun findTopByBehandlingIdOrderByEndretTidDesc(behandlingId: BehandlingId): Behandlingshistorikk

    fun findTopByBehandlingIdAndStegOrderByEndretTidDesc(behandlingId: BehandlingId, steg: StegType): Behandlingshistorikk?
}
