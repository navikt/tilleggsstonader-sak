package no.nav.tilleggsstonader.sak.brev.mellomlager

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.data.relational.core.sql.LockMode
import org.springframework.data.relational.repository.Lock
import org.springframework.stereotype.Repository

@Repository
interface MellomlagerBrevRepository :
    RepositoryInterface<MellomlagretBrev, BehandlingId>, InsertUpdateRepository<MellomlagretBrev> {

    @Lock(LockMode.PESSIMISTIC_WRITE)
    fun findByBehandlingId(behandlingId: BehandlingId): MellomlagretBrev
}
