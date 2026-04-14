package no.nav.tilleggsstonader.sak.infrastruktur.database.repository

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import java.util.UUID

class VilkårperiodeRepositoryFake :
    DummyRepository<Vilkårperiode, UUID>({ it.id }),
    VilkårperiodeRepository {
    override fun findByBehandlingId(behandlingId: BehandlingId): List<Vilkårperiode> = findAll().filter { it.behandlingId == behandlingId }

    override fun findByBehandlingIdAndGlobalId(
        behandlingId: BehandlingId,
        globalId: VilkårperiodeGlobalId,
    ): Vilkårperiode? = findByBehandlingId(behandlingId).find { it.globalId == globalId }

    override fun findByBehandlingIdAndResultat(
        behandlingId: BehandlingId,
        resultat: ResultatVilkårperiode,
    ): List<Vilkårperiode> = findByBehandlingId(behandlingId).filter { it.resultat == resultat }

    override fun findByBehandlingIdAndResultatNot(
        behandlingId: BehandlingId,
        resultat: ResultatVilkårperiode,
    ): List<Vilkårperiode> = findByBehandlingId(behandlingId).filter { it.resultat != resultat }
}
