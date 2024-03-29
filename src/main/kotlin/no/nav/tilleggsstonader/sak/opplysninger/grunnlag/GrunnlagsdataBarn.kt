package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Navn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlPersonForelderBarn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gjeldende
import no.nav.tilleggsstonader.sak.util.antallÅrSiden
import java.time.LocalDate

data class GrunnlagsdataBarn(
    val ident: String,
    val navn: Navn,
    val alder: Int?,
    val dødsdato: LocalDate?,
)

fun Map<String, PdlPersonForelderBarn>.tilGrunnlagsdataBarn() = entries.map { (ident, barn) ->
    GrunnlagsdataBarn(
        ident = ident,
        navn = barn.navn.gjeldende(),
        alder = antallÅrSiden(barn.fødsel.gjeldende().fødselsdato),
        dødsdato = barn.dødsfall.gjeldende()?.dødsdato,
    )
}
