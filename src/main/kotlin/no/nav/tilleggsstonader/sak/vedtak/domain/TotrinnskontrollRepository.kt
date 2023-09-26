package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TotrinnskontrollRepository : RepositoryInterface<Totrinnsstatus, UUID>, InsertUpdateRepository<Totrinnsstatus> {

    fun findLastBehandlingIdOrderBySporbarEndretEndretTid(behandlingId: UUID): Totrinnsstatus

   // fun findLastTotrinnskontrollByBehandlingIdAndStatusOrderByEndretTid(behandlingId: EksternBehandlingId, string: String): Totrinnskontroll

   // fun findAllByBehandlingId(behandlingId: EksternBehandlingId): List<Totrinnskontroll>





}
