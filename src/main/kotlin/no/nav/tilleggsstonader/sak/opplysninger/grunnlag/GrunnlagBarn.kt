package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlPersonForelderBarn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gjeldende
import no.nav.tilleggsstonader.sak.util.antallÅrSiden
import java.time.LocalDate

data class GrunnlagBarn(
    val ident: String,
    val navn: Navn,
    val fødselsdato: LocalDate?,
    val alder: Int?,
    val dødsdato: LocalDate?,
)

fun Map<String, PdlPersonForelderBarn>.tilGrunnlagsdataBarn() = entries.map { (ident, barn) ->
    GrunnlagBarn(
        ident = ident,
        navn = barn.navn.gjeldende().tilNavn(),
        fødselsdato = barn.fødselsdato.gjeldende().fødselsdato,
        alder = antallÅrSiden(barn.fødselsdato.gjeldende().fødselsdato),
        dødsdato = barn.dødsfall.gjeldende()?.dødsdato,
    )
}
