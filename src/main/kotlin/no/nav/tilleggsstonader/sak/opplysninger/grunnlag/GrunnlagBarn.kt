package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Familierelasjonsrolle
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.MotpartsRolle
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Navn
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
    val foreldre: List<Foreldre>
)

data class Foreldre(val ident: String)

fun Map<String, PdlPersonForelderBarn>.tilGrunnlagsdataBarn() = entries.map { (ident, barn) ->
    GrunnlagBarn(
        ident = ident,
        navn = barn.navn.gjeldende(),
        fødselsdato = barn.fødsel.gjeldende().fødselsdato,
        alder = antallÅrSiden(barn.fødsel.gjeldende().fødselsdato),
        dødsdato = barn.dødsfall.gjeldende()?.dødsdato,
        foreldre = mapForeldre(barn)
    )
}

private fun mapForeldre(barn: PdlPersonForelderBarn): List<Foreldre> {
    return barn.forelderBarnRelasjon
        .filter { it.minRolleForPerson == Familierelasjonsrolle.BARN }
        .mapNotNull { it.relatertPersonsIdent }
        .map { Foreldre(ident = it) }
}
