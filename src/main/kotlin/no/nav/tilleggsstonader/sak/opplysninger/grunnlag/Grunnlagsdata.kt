package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
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
    val fødsel: Fødsel?,
    val barn: List<GrunnlagBarn>,
    val arena: GrunnlagArena? = null,
)

data class GrunnlagArena(
    val vedtakTom: LocalDate?,
)

data class Fødsel(
    val fødselsdato: LocalDate?,
    val fødselsår: Int,
) {
    fun fødselsdatoEller1JanForFødselsår() =
        fødselsdato
            ?: LocalDate.of(fødselsår, 1, 1)
}

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
