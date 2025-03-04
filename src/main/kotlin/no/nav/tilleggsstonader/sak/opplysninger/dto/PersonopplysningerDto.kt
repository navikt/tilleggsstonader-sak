package no.nav.tilleggsstonader.sak.opplysninger.dto

import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Navn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.visningsnavn
import java.time.LocalDate
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering as AdressebeskyttelseGraderingPdl

data class PersonopplysningerDto(
    val personIdent: String,
    val navn: NavnDto,
    val harVergemål: Boolean,
    val harFullmektig: Boolean,
    val adressebeskyttelse: Adressebeskyttelse,
    val erSkjermet: Boolean,
    val dødsdato: LocalDate?,
)

data class StatsborgerskapDto(
    val land: String,
    val gyldigFraOgMedDato: LocalDate?,
    val gyldigTilOgMedDato: LocalDate?,
)

data class UtflyttingDto(
    val tilflyttingsland: String?,
    val dato: LocalDate?,
    val tilflyttingssted: String? = null,
)

data class InnflyttingDto(
    val fraflyttingsland: String?,
    val dato: LocalDate?,
    val fraflyttingssted: String? = null,
)

data class OppholdstillatelseDto(
    val oppholdstillatelse: OppholdType,
    val fraDato: LocalDate?,
    val tilDato: LocalDate?,
)

enum class OppholdType {
    PERMANENT,
    MIDLERTIDIG,
    UKJENT,
}

data class BarnDto(
    val personIdent: String,
    val navn: String,
    val annenForelder: AnnenForelderMinimumDto?,
    val adresse: List<AdresseDto>,
    val borHosSøker: Boolean,
    val fødselsdato: LocalDate?,
    val dødsdato: LocalDate?,
)

data class BarnMinimumDto(
    val personIdent: String,
    val navn: String,
    val fødselsdato: LocalDate?,
)

data class AnnenForelderMinimumDto(
    val personIdent: String,
    val navn: String,
    val dødsdato: LocalDate?,
    val bostedsadresse: String?,
)

data class AdresseDto(
    val visningsadresse: String?,
    val type: AdresseType,
    val gyldigFraOgMed: LocalDate?,
    val gyldigTilOgMed: LocalDate?,
    val angittFlyttedato: LocalDate? = null,
    val erGjeldende: Boolean = false,
)

enum class AdresseType(
    val rekkefølge: Int,
) {
    BOSTEDADRESSE(1),
    OPPHOLDSADRESSE(2),
    KONTAKTADRESSE(3),
    KONTAKTADRESSE_UTLAND(4),
}

enum class Adressebeskyttelse {
    STRENGT_FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
    FORTROLIG,
    UGRADERT,
    ;

    companion object {
        fun fraPdl(status: AdressebeskyttelseGraderingPdl) = Adressebeskyttelse.valueOf(status.name)
    }
}

@Suppress("unused")
enum class Folkeregisterpersonstatus(
    private val pdlStatus: String,
    val visningsnavn: String,
) {
    BOSATT("bosatt", "Bosatt"),
    UTFLYTTET("utflyttet", "Utflyttet"),
    FORSVUNNET("forsvunnet", "Forsvunnet"),
    DØD("doed", "Død"),
    OPPHØRT("opphoert", "Opphørt"),
    FØDSELSREGISTRERT("foedselsregistrert", "Fødselsregistrert"),
    MIDLERTIDIG("midlertidig", "Midlertidig"),
    INAKTIV("inaktiv", "Inaktiv"),
    UKJENT("ukjent", "Ukjent"),
    ;

    companion object {
        private val map = entries.associateBy(Folkeregisterpersonstatus::pdlStatus)

        fun fraPdl(status: no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Folkeregisterpersonstatus) =
            map.getOrDefault(status.status, UKJENT)
    }
}

data class NavnDto(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val visningsnavn: String,
) {
    companion object {
        fun fraNavn(navn: Navn): NavnDto = NavnDto(navn.fornavn, navn.mellomnavn, navn.etternavn, navn.visningsnavn())
    }
}

data class VergemålDto(
    val embete: String?,
    val type: String?,
    val motpartsPersonident: String?,
    val navn: String?,
    val omfang: String?,
)
