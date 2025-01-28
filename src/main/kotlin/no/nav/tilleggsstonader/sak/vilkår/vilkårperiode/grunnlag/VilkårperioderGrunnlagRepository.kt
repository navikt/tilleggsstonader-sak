package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository

@Repository
interface VilkårperioderGrunnlagRepository :
    RepositoryInterface<VilkårperioderGrunnlagDomain, BehandlingId>,
    InsertUpdateRepository<VilkårperioderGrunnlagDomain> {
    fun findByBehandlingId(behandlingId: BehandlingId): VilkårperioderGrunnlagDomain?
}
