package no.nav.tilleggsstonader.sak.opplysninger.dto

import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Navn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.visningsnavn
import java.time.LocalDate

data class PersonopplysningerDto(
    val personIdent: String,
    val navn: NavnDto,
    val kjønn: Kjønn,
    val adressebeskyttelse: Adressebeskyttelse?,
    val folkeregisterpersonstatus: Folkeregisterpersonstatus?,
    val fødselsdato: LocalDate?,
    val dødsdato: LocalDate?,
    val statsborgerskap: List<StatsborgerskapDto>,
    val sivilstand: List<SivilstandDto>,
    val adresse: List<AdresseDto>,
    val fullmakt: List<FullmaktDto>,
    val egenAnsatt: Boolean,
    val barn: List<BarnDto>,
    val innflyttingTilNorge: List<InnflyttingDto>,
    val utflyttingFraNorge: List<UtflyttingDto>,
    val oppholdstillatelse: List<OppholdstillatelseDto>,
    val vergemål: List<VergemålDto>,
)

data class StatsborgerskapDto(
    val land: String,
    val gyldigFraOgMedDato: LocalDate?,
    val gyldigTilOgMedDato: LocalDate?,
)

data class UtflyttingDto(val tilflyttingsland: String?, val dato: LocalDate?, val tilflyttingssted: String? = null)

data class InnflyttingDto(val fraflyttingsland: String?, val dato: LocalDate?, val fraflyttingssted: String? = null)

data class OppholdstillatelseDto(val oppholdstillatelse: OppholdType, val fraDato: LocalDate?, val tilDato: LocalDate?)

enum class OppholdType {
    PERMANENT,
    MIDLERTIDIG,
    UKJENT,
}

data class DeltBostedDto(
    val startdatoForKontrakt: LocalDate,
    val sluttdatoForKontrakt: LocalDate?,
    val historisk: Boolean,
)

data class BarnDto(
    val personIdent: String,
    val navn: String,
    val annenForelder: AnnenForelderMinimumDto?,
    val adresse: List<AdresseDto>,
    val borHosSøker: Boolean,
    val deltBosted: List<DeltBostedDto>,
    val harDeltBostedNå: Boolean,
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

data class SivilstandDto(
    val type: Sivilstandstype,
    val gyldigFraOgMed: LocalDate?,
    val relatertVedSivilstand: String?,
    val navn: String?,
    val dødsdato: LocalDate?,
    val erGjeldende: Boolean,
)

@Suppress("unused") // Kopi fra PDL
enum class Sivilstandstype {

    UOPPGITT,
    UGIFT,
    GIFT,
    ENKE_ELLER_ENKEMANN,
    SKILT,
    SEPARERT,
    REGISTRERT_PARTNER,
    SEPARERT_PARTNER,
    SKILT_PARTNER,
    GJENLEVENDE_PARTNER,
    ;

    fun erGift(): Boolean = this == REGISTRERT_PARTNER || this == GIFT
    fun erUgiftEllerUoppgitt(): Boolean = this == UGIFT || this == UOPPGITT
    fun erSeparert(): Boolean = this == SEPARERT_PARTNER || this == SEPARERT
    fun erEnkeEllerEnkemann(): Boolean = this == ENKE_ELLER_ENKEMANN || this == GJENLEVENDE_PARTNER
    fun erSkilt(): Boolean = this == SKILT || this == SKILT_PARTNER
}

data class AdresseDto(
    val visningsadresse: String?,
    val type: AdresseType,
    val gyldigFraOgMed: LocalDate?,
    val gyldigTilOgMed: LocalDate?,
    val angittFlyttedato: LocalDate? = null,
    val erGjeldende: Boolean = false,
)

enum class AdresseType(val rekkefølge: Int) {
    BOSTEDADRESSE(1),
    OPPHOLDSADRESSE(2),
    KONTAKTADRESSE(3),
    KONTAKTADRESSE_UTLAND(4),
}

data class FullmaktDto(
    val gyldigFraOgMed: LocalDate,
    val gyldigTilOgMed: LocalDate,
    val motpartsPersonident: String,
    val navn: String?,
    val områder: List<String>,
)

@Suppress("unused") // Kopi fra PDL
enum class Adressebeskyttelse {

    STRENGT_FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
    FORTROLIG,
    UGRADERT,
}

@Suppress("unused")
enum class Folkeregisterpersonstatus(private val pdlStatus: String, val visningsnavn: String) {

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

        private val map = values().associateBy(Folkeregisterpersonstatus::pdlStatus)
        fun fraPdl(status: no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Folkeregisterpersonstatus) =
            map.getOrDefault(status.status, UKJENT)
    }
}

@Suppress("unused") // Kopi fra PDL
enum class Kjønn {

    KVINNE,
    MANN,
    UKJENT,
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
