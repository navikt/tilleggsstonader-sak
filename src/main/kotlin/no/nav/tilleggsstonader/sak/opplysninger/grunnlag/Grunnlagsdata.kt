package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Grunnlagsdata(
    @Id
    val behandlingId: UUID,
    val grunnlag: Grunnlag,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

data class Grunnlag(
    val navn: Navn,
    val barn: List<GrunnlagBarn>,
)

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
) {
    fun visningsnavn(): String {
        return if (mellomnavn == null) {
            "$fornavn $etternavn"
        } else {
            "$fornavn $mellomnavn $etternavn"
        }
    }
}
