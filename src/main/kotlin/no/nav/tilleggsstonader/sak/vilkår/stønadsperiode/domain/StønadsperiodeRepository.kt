package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StønadsperiodeRepository : RepositoryInterface<Stønadsperiode, UUID>, InsertUpdateRepository<Stønadsperiode> {

    fun findAllByBehandlingId(behandlingId: BehandlingId): List<Stønadsperiode>
}
