package no.nav.tilleggsstonader.sak.behandling.historikk.domain

import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BehandlingshistorikkRepository :
    RepositoryInterface<Behandlingshistorikk, UUID>,
    InsertUpdateRepository<Behandlingshistorikk> {

    fun findByBehandlingIdOrderByEndretTidDesc(behandlingId: UUID): List<Behandlingshistorikk>

    fun findByBehandlingIdOrderByEndretTidAsc(behandlingId: UUID): List<Behandlingshistorikk>

    fun findTopByBehandlingIdOrderByEndretTidDesc(behandlingId: UUID): Behandlingshistorikk

    fun findTopByBehandlingIdAndStegOrderByEndretTidDesc(behandlingId: UUID, steg: StegType): Behandlingshistorikk?
}
