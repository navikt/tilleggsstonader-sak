package no.nav.tilleggsstonader.sak.brev

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository

@Repository
interface VedtaksbrevRepository : RepositoryInterface<Vedtaksbrev, BehandlingId>, InsertUpdateRepository<Vedtaksbrev>
