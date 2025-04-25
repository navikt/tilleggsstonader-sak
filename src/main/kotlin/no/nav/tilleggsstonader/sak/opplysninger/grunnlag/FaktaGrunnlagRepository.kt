package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktaGrunnlagId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.TypeFaktaGrunnlag
import org.springframework.stereotype.Repository

@Repository
interface FaktaGrunnlagRepository :
    RepositoryInterface<FaktaGrunnlag, FaktaGrunnlagId>,
    InsertUpdateRepository<FaktaGrunnlag> {
    fun findByBehandlingIdAndType(
        behandlingId: BehandlingId,
        type: TypeFaktaGrunnlag,
    ): List<FaktaGrunnlag>

    fun findByBehandlingIdAndTypeIn(
        behandlingId: BehandlingId,
        typer: List<TypeFaktaGrunnlag>,
    ): List<FaktaGrunnlag>

    fun existsByBehandlingId(behandlingId: BehandlingId): Boolean
}
