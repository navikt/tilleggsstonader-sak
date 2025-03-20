package no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktaGrunnlagId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository

@Repository
interface FaktaGrunnlagRepository :
    RepositoryInterface<FaktaGrunnlag, FaktaGrunnlagId>,
    InsertUpdateRepository<FaktaGrunnlag> {
    fun findByBehandlingIdAndType(
        behandlingId: BehandlingId,
        type: TypeFaktaGrunnlag,
    ): List<FaktaGrunnlag>
}
