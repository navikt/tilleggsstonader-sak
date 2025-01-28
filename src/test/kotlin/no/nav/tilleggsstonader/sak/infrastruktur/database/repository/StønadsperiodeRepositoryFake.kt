package no.nav.tilleggsstonader.sak.infrastruktur.database.repository

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import java.util.UUID

class StønadsperiodeRepositoryFake :
    DummyRepository<Stønadsperiode, UUID>({ it.id }),
    StønadsperiodeRepository {
    override fun findAllByBehandlingId(behandlingId: BehandlingId): List<Stønadsperiode> =
        findAll().filter { it.behandlingId == behandlingId }
}
