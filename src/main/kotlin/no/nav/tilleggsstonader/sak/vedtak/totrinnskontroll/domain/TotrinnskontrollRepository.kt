package no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TotrinnskontrollRepository :
    RepositoryInterface<Totrinnskontroll, UUID>,
    InsertUpdateRepository<Totrinnskontroll> {

    fun findTopByBehandlingIdAndStatusOrderBySporbarEndretEndretTidDesc(
        behandlingId: UUID,
        status: TotrinnsKontrollStatus,
    ): Totrinnskontroll

    fun findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(behandlingId: UUID): Totrinnskontroll

    // TODO trengs denne?
    fun findAllByBehandlingId(behandlingId: UUID): List<Totrinnskontroll>
}
