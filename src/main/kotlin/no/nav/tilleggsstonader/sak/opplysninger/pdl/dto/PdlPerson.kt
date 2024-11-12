package no.nav.tilleggsstonader.sak.opplysninger.pdl.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.LocalDateTime

data class PdlResponse<T>(
    val data: T,
    val errors: List<PdlError>?,
    val extensions: PdlExtensions?,
) {

    fun harFeil(): Boolean {
        return errors != null && errors.isNotEmpty()
    }
    fun harAdvarsel(): Boolean {
        return !extensions?.warnings.isNullOrEmpty()
    }
    fun errorMessages(): String {
        return errors?.joinToString { it -> it.message } ?: ""
    }
}

data class PdlBolkResponse<T>(val data: PersonBolk<T>?, val errors: List<PdlError>?, val extensions: PdlExtensions?) {

    fun errorMessages(): String {
        return errors?.joinToString { it -> it.message } ?: ""
    }
    fun harAdvarsel(): Boolean {
        return !extensions?.warnings.isNullOrEmpty()
    }
}

data class PdlError(
    val message: String,
    val extensions: PdlErrorExtensions?,
)

data class PdlErrorExtensions(val code: String?) {

    fun notFound() = code == "not_found"
}
data class PdlExtensions(val warnings: List<PdlWarning>?)
data class PdlWarning(val details: Any?, val id: String?, val message: String?, val query: String?)

data class PdlSøkerData(val person: PdlSøker?)

data class PersonDataBolk<T>(val ident: String, val code: String, val person: T?)
data class PersonBolk<T>(val personBolk: List<PersonDataBolk<T>>)

interface PdlPerson {

    val fødselsdato: List<Fødselsdato>
    val bostedsadresse: List<Bostedsadresse>
}

data class PdlIdentBolkResponse(
    val data: IdentBolk?,
    val errors: List<PdlError>?,
) {

    fun errorMessages(): String {
        return errors?.joinToString { it -> it.message } ?: ""
    }
}

data class PdlIdenterBolk(
    val code: String,
    val ident: String,
    val identer: List<PdlIdent>?,
) {

    fun gjeldende(): PdlIdent = this.identer?.first { !it.historisk } ?: PdlIdent(ident, false)
}

data class IdentBolk(val hentIdenterBolk: List<PdlIdenterBolk>)

data class PdlIdent(val ident: String, val historisk: Boolean)

data class PdlIdenter(val identer: List<PdlIdent>) {

    fun gjeldende(): PdlIdent = this.identer.first { !it.historisk }
}

data class PdlHentIdenter(val hentIdenter: PdlIdenter?)

data class PdlPersonKort(
    val adressebeskyttelse: List<Adressebeskyttelse>,
    val navn: List<Navn>,
    @JsonProperty("doedsfall") val dødsfall: List<Dødsfall>,
)

data class PdlSøker(
    val adressebeskyttelse: List<Adressebeskyttelse>,
    override val bostedsadresse: List<Bostedsadresse>,
    @JsonProperty("doedsfall") val dødsfall: List<Dødsfall>,
    val forelderBarnRelasjon: List<ForelderBarnRelasjon>,
    val folkeregisteridentifikator: List<Folkeregisteridentifikator>,
    @JsonProperty("foedselsdato") override val fødselsdato: List<Fødselsdato>,
    val folkeregisterpersonstatus: List<Folkeregisterpersonstatus>,
    val kontaktadresse: List<Kontaktadresse>,
    val navn: List<Navn>,
    val opphold: List<Opphold>,
    val oppholdsadresse: List<Oppholdsadresse>,
    val statsborgerskap: List<Statsborgerskap>,
    val innflyttingTilNorge: List<InnflyttingTilNorge>,
    val utflyttingFraNorge: List<UtflyttingFraNorge>,
    val vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt>,
) : PdlPerson {

    fun alleIdenter(): Set<String> = folkeregisteridentifikator.map { it.ident }.toSet()
}

data class PdlBarn(
    val adressebeskyttelse: List<Adressebeskyttelse>,
    override val bostedsadresse: List<Bostedsadresse>,
    @JsonProperty("doedsfall") val dødsfall: List<Dødsfall>,
    val forelderBarnRelasjon: List<ForelderBarnRelasjon>,
    @JsonProperty("foedselsdato") override val fødselsdato: List<Fødselsdato>,
    val navn: List<Navn>,
) : PdlPerson

data class PdlAnnenForelder(
    val adressebeskyttelse: List<Adressebeskyttelse>,
    override val bostedsadresse: List<Bostedsadresse>,
    @JsonProperty("doedsfall") val dødsfall: List<Dødsfall>,
    @JsonProperty("foedselsdato") override val fødselsdato: List<Fødselsdato>,
    val folkeregisteridentifikator: List<Folkeregisteridentifikator>,
    val navn: List<Navn>,
) : PdlPerson

data class Metadata(val historisk: Boolean)

data class Folkeregistermetadata(
    val gyldighetstidspunkt: LocalDateTime?,
    @JsonProperty("opphoerstidspunkt") val opphørstidspunkt: LocalDateTime?,
)

data class Folkeregisteridentifikator(
    @JsonProperty("identifikasjonsnummer")
    val ident: String,
    val status: FolkeregisteridentifikatorStatus,
    val metadata: Metadata,
)

enum class FolkeregisteridentifikatorStatus {
    I_BRUK,
    OPPHOERT,
}

data class Bostedsadresse(
    val angittFlyttedato: LocalDate?,
    val gyldigFraOgMed: LocalDate?,
    val gyldigTilOgMed: LocalDate?,
    val coAdressenavn: String?,
    val utenlandskAdresse: UtenlandskAdresse?,
    val vegadresse: Vegadresse?,
    val ukjentBosted: UkjentBosted?,
    val matrikkeladresse: Matrikkeladresse?,
    val metadata: Metadata,
) {

    val matrikkelId get() = matrikkeladresse?.matrikkelId ?: vegadresse?.matrikkelId

    val bruksenhetsnummer get() = matrikkeladresse?.bruksenhetsnummer ?: vegadresse?.bruksenhetsnummer
}

data class Oppholdsadresse(
    val gyldigFraOgMed: LocalDate?,
    val gyldigTilOgMed: LocalDate? = null,
    val coAdressenavn: String?,
    val utenlandskAdresse: UtenlandskAdresse?,
    val vegadresse: Vegadresse?,
    val oppholdAnnetSted: String?,
    val metadata: Metadata,
)

data class Kontaktadresse(
    val coAdressenavn: String?,
    val gyldigFraOgMed: LocalDate?,
    val gyldigTilOgMed: LocalDate?,
    val postadresseIFrittFormat: PostadresseIFrittFormat?,
    val postboksadresse: Postboksadresse?,
    val type: KontaktadresseType,
    val utenlandskAdresse: UtenlandskAdresse?,
    val utenlandskAdresseIFrittFormat: UtenlandskAdresseIFrittFormat?,
    val vegadresse: Vegadresse?,
)

@Suppress("unused")
enum class KontaktadresseType {

    @JsonProperty("Innland")
    INNLAND,

    @JsonProperty("Utland")
    UTLAND,
}

data class Postboksadresse(
    val postboks: String,
    val postbokseier: String?,
    val postnummer: String?,
)

data class PostadresseIFrittFormat(
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val postnummer: String?,
)

data class Vegadresse(
    val husnummer: String?,
    val husbokstav: String?,
    val bruksenhetsnummer: String?,
    val adressenavn: String?,
    val kommunenummer: String?,
    val tilleggsnavn: String?,
    val postnummer: String?,
    val matrikkelId: Long?,
)

data class UkjentBosted(val bostedskommune: String?)

data class Adressebeskyttelse(val gradering: AdressebeskyttelseGradering, val metadata: Metadata) {

    fun erStrengtFortrolig(): Boolean = this.gradering == AdressebeskyttelseGradering.STRENGT_FORTROLIG ||
        this.gradering == AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
}

enum class AdressebeskyttelseGradering {
    STRENGT_FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
    FORTROLIG,
    UGRADERT,
}

fun AdressebeskyttelseGradering.tilDiskresjonskode(): String? = when (this) {
    AdressebeskyttelseGradering.STRENGT_FORTROLIG -> "SPSF" // Kode 6
    AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> "SPSF" // Kode 19
    AdressebeskyttelseGradering.FORTROLIG -> "SPFO" // Kode 7
    AdressebeskyttelseGradering.UGRADERT -> null
}

/**
 * @param [fødselsår] skal finnes på alle brukere, men er definiert som nullable i skjema
 */
data class Fødselsdato(
    @JsonProperty("foedselsaar") val fødselsår: Int?,
    @JsonProperty("foedselsdato") val fødselsdato: LocalDate?,
    val metadata: Metadata,
)

data class Dødsfall(@JsonProperty("doedsdato") val dødsdato: LocalDate?)

data class ForelderBarnRelasjon(
    val relatertPersonsIdent: String?,
    val relatertPersonsRolle: Familierelasjonsrolle,
    val minRolleForPerson: Familierelasjonsrolle?,
)

enum class Familierelasjonsrolle {
    BARN,
    MOR,
    FAR,
    MEDMOR,
}

data class Folkeregisterpersonstatus(
    val status: String,
    val forenkletStatus: String,
    val metadata: Metadata,
)

enum class MotpartsRolle {
    FULLMAKTSGIVER,
    FULLMEKTIG,
}

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val metadata: Metadata,
)

data class Personnavn(
    val etternavn: String,
    val fornavn: String,
    val mellomnavn: String?,
)

data class Tolk(@JsonProperty("spraak") val språk: String?)

data class Statsborgerskap(
    val land: String,
    val gyldigFraOgMed: LocalDate?,
    val gyldigTilOgMed: LocalDate?,
)

data class Opphold(
    val type: Oppholdstillatelse,
    val oppholdFra: LocalDate?,
    val oppholdTil: LocalDate?,
)

enum class Oppholdstillatelse {
    MIDLERTIDIG,
    PERMANENT,
    OPPLYSNING_MANGLER,
}

data class InnflyttingTilNorge(
    val fraflyttingsland: String?,
    val fraflyttingsstedIUtlandet: String?,
    val folkeregistermetadata: Folkeregistermetadata,
)

data class UtflyttingFraNorge(
    val tilflyttingsland: String?,
    val tilflyttingsstedIUtlandet: String?,
    val utflyttingsdato: LocalDate?,
    val folkeregistermetadata: Folkeregistermetadata,
)

data class UtenlandskAdresse(
    val adressenavnNummer: String?,
    val bySted: String?,
    val bygningEtasjeLeilighet: String?,
    val landkode: String,
    val postboksNummerNavn: String?,
    val postkode: String?,
    val regionDistriktOmraade: String?,
)

data class Matrikkeladresse(
    val matrikkelId: Long?,
    val bruksenhetsnummer: String?,
    val tilleggsnavn: String?,
    val postnummer: String?,
)

data class UtenlandskAdresseIFrittFormat(
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val byEllerStedsnavn: String?,
    val landkode: String,
    val postkode: String?,
)

data class VergeEllerFullmektig(
    val identifiserendeInformasjon: IdentifiserendeInformasjon?,
    val motpartsPersonident: String?,
    val omfang: String?,
    val omfangetErInnenPersonligOmraade: Boolean,
)

data class IdentifiserendeInformasjon(
    val navn: Personnavn?,
)

data class VergemaalEllerFremtidsfullmakt(
    val embete: String?,
    val folkeregistermetadata: Folkeregistermetadata?,
    val type: String?,
    val vergeEllerFullmektig: VergeEllerFullmektig,
)
