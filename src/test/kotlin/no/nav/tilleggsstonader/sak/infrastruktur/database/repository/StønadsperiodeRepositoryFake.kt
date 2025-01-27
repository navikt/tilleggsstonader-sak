package no.nav.tilleggsstonader.sak.infrastruktur.database.repository

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import java.util.UUID

class StønadsperiodeRepositoryFake : StønadsperiodeRepository, DummyRepository<Stønadsperiode, UUID>({ it.id }) {

    override fun findAllByBehandlingId(behandlingId: BehandlingId): List<Stønadsperiode> {
        return findAll().filter { it.behandlingId == behandlingId }
    }
}
