package no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag

import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlBarn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gjeldende
import no.nav.tilleggsstonader.sak.util.antallÅrSiden
import java.time.LocalDate

data class FaktaGrunnlagPersonopplysninger(
    val navn: Navn,
    val fødsel: Fødsel?,
    val barn: List<GrunnlagBarn>,
) : FaktaGrunnlagData {
    override val type: TypeFaktaGrunnlag = TypeFaktaGrunnlag.PERSONOPPLYSNINGER
}

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
    fun visningsnavn(): String =
        if (mellomnavn == null) {
            "$fornavn $etternavn"
        } else {
            "$fornavn $mellomnavn $etternavn"
        }
}

data class GrunnlagBarn(
    val ident: String,
    val navn: Navn,
    val fødselsdato: LocalDate?,
    val alder: Int?,
    val dødsdato: LocalDate?,
)

fun Map<String, PdlBarn>.tilGrunnlagsdataBarn() =
    entries.map { (ident, barn) ->
        GrunnlagBarn(
            ident = ident,
            navn = barn.navn.gjeldende().tilNavn(),
            fødselsdato = barn.fødselsdato.gjeldende().fødselsdato,
            alder = antallÅrSiden(barn.fødselsdato.gjeldende().fødselsdato),
            dødsdato = barn.dødsfall.gjeldende()?.dødsdato,
        )
    }

fun no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Navn.tilNavn() =
    Navn(
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn,
    )
