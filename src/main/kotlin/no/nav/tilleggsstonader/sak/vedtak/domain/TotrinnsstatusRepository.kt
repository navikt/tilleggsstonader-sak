package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TotrinnsstatusRepository : RepositoryInterface<TotrinnsKontroll, UUID>, InsertUpdateRepository<TotrinnsKontroll> {

    fun findTopByBehandlingIdAndStatusOrderBySporbarEndretEndretTidDesc(behandlingId: UUID, status: TotrinnsStatus): TotrinnsKontroll

    fun findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(behandlingId: UUID): TotrinnsKontroll
    fun findAllByBehandlingId(behandlingId: UUID): List<TotrinnsKontroll>
}
