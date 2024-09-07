package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface VilkårperiodeRepository : RepositoryInterface<Vilkårperiode, UUID>, InsertUpdateRepository<Vilkårperiode> {

    fun findByBehandlingId(behandlingId: BehandlingId): List<Vilkårperiode>

    fun findByBehandlingIdAndResultat(behandlingId: BehandlingId, resultat: ResultatVilkårperiode): List<Vilkårperiode>

    fun findByBehandlingIdAndResultatNot(behandlingId: BehandlingId, resultat: ResultatVilkårperiode): List<Vilkårperiode>
}
