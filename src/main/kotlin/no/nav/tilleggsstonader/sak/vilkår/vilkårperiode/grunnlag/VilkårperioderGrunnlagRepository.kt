package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface VilkårperioderGrunnlagRepository : RepositoryInterface<VilkårperioderGrunnlagDomain, UUID>, InsertUpdateRepository<VilkårperioderGrunnlagDomain> {

    fun findByBehandlingId(behandlingId: UUID): VilkårperioderGrunnlagDomain?
}
