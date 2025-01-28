package no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TotrinnskontrollRepository :
    RepositoryInterface<Totrinnskontroll, UUID>,
    InsertUpdateRepository<Totrinnskontroll> {
    fun findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(behandlingId: BehandlingId): Totrinnskontroll?
}
