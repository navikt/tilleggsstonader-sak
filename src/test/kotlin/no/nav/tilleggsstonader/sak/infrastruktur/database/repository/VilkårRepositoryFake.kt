package no.nav.tilleggsstonader.sak.infrastruktur.database.repository

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import java.time.LocalDateTime
import java.util.UUID

class VilkårRepositoryFake :
    DummyRepository<Vilkår, VilkårId>({ it.id }),
    VilkårRepository {
    override fun findByBehandlingId(behandlingId: BehandlingId): List<Vilkår> = findAll().filter { it.behandlingId == behandlingId }

    override fun deleteByBehandlingId(behandlingId: BehandlingId) {
        deleteAll(findByBehandlingId(behandlingId))
    }

    override fun oppdaterEndretTid(
        id: VilkårId,
        nyttTidspunkt: LocalDateTime,
    ) {
        TODO("Not yet implemented")
    }

    override fun settMaskinelltOpprettet(id: VilkårId) {
        TODO("Not yet implemented")
    }

    override fun findByTypeAndBehandlingIdIn(
        vilkårtype: VilkårType,
        behandlingIds: Collection<UUID>,
    ): List<Vilkår> {
        TODO("Not yet implemented")
    }
}
