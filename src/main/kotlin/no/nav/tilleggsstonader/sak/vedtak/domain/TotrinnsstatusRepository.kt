package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TotrinnsstatusRepository : RepositoryInterface<Totrinnsstatus, UUID>, InsertUpdateRepository<Totrinnsstatus> {

    fun findTopByBehandlingIdAndStatusOrderBySporbarEndretEndretTidDesc(behandlingId: UUID, status: TotrinnsKontrollStatus): Totrinnsstatus

    fun findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(behandlingId: UUID): Totrinnsstatus

    fun findAllByBehandlingId(behandlingId: UUID): List<Totrinnsstatus>
}
