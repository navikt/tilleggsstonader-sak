package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FødselFaktaGrunnlag
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.GrunnlagBarn
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.Navn
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate

data class Grunnlagsdata(
    @Id
    val behandlingId: BehandlingId,
    val grunnlag: Grunnlag,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

data class Grunnlag(
    val navn: Navn,
    val fødsel: FødselFaktaGrunnlag?,
    val barn: List<GrunnlagBarn>,
    val arena: GrunnlagArena? = null,
)

data class GrunnlagArena(
    val vedtakTom: LocalDate?,
)
