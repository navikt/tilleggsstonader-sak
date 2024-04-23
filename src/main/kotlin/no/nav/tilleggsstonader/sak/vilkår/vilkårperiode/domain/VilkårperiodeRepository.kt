package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface VilkårperiodeRepository : RepositoryInterface<Vilkårperiode, UUID>, InsertUpdateRepository<Vilkårperiode> {

    fun findByBehandlingId(behandlingId: UUID): List<Vilkårperiode>

    fun findByBehandlingIdAndResultat(behandlingId: UUID, resultat: ResultatVilkårperiode): List<Vilkårperiode>
}
