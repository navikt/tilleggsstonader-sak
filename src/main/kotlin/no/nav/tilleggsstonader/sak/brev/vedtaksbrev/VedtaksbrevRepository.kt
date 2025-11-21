package no.nav.tilleggsstonader.sak.brev.vedtaksbrev

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import org.springframework.stereotype.Repository

@Repository
interface VedtaksbrevRepository :
    RepositoryInterface<Vedtaksbrev, BehandlingId>,
    InsertUpdateRepository<Vedtaksbrev> {
    fun findByBehandlingId(behandlingId: BehandlingId): Vedtaksbrev?
}
