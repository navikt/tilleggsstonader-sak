package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TotrinnskontrollRepository : RepositoryInterface<Totrinnsstatus, UUID>, InsertUpdateRepository<Totrinnsstatus> {

    fun findTopByBehandlingIdAndStatusOrderBySporbarEndretEndretTidDesc(behandlingId: UUID, status: String): Totrinnsstatus

    fun findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(behandlingId: UUID): Totrinnsstatus
    fun findAllByBehandlingId(behandlingId: UUID): List<Totrinnsstatus>
}
