package no.nav.tilleggsstonader.sak.opplysninger.pdl

import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Adressebeskyttelse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Bostedsadresse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Dødsfall
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Familierelasjonsrolle
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Folkeregisteridentifikator
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.FolkeregisteridentifikatorFraSøk
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.FolkeregisteridentifikatorStatus
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Folkeregistermetadata
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Folkeregisterpersonstatus
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.ForelderBarnRelasjon
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Fødselsdato
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.IdentifiserendeInformasjon
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.InnflyttingTilNorge
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Kontaktadresse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.KontaktadresseType
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Matrikkeladresse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Metadata
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Navn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Opphold
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Oppholdsadresse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Oppholdstillatelse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlAnnenForelder
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlBarn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlPersonFraSøk
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlPersonKort
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlSøker
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlSøkerData
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PersonBolk
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PersonDataBolk
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PersonSøk
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PersonSøkResultat
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PersonSøkTreff
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Personnavn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PostadresseIFrittFormat
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Postboksadresse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Statsborgerskap
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.UkjentBosted
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.UtenlandskAdresse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.UtenlandskAdresseIFrittFormat
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.UtflyttingFraNorge
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Vegadresse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.VergeEllerFullmektig
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.VergemaalEllerFremtidsfullmakt
import java.time.LocalDate
import java.time.LocalDateTime

object PdlTestdata {
    private val metadataGjeldende = Metadata(false)

    val vegadresse =
        Vegadresse(
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            1L,
        )

    private val matrikkeladresse = Matrikkeladresse(1L, "", "", "")
    private val utenlandskAdresse = UtenlandskAdresse("", "", "", "", "", "", "")

    private val folkeregistermetadata = Folkeregistermetadata(LocalDateTime.now(), LocalDateTime.now())

    private val navn = listOf(Navn("", "", "", metadataGjeldende))

    private val adressebeskyttelse =
        listOf(Adressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG, metadataGjeldende))

    private val bostedsadresse =
        listOf(
            Bostedsadresse(
                angittFlyttedato = LocalDate.now().minusDays(10),
                gyldigFraOgMed = LocalDate.now(),
                gyldigTilOgMed = LocalDate.now(),
                coAdressenavn = "",
                utenlandskAdresse = utenlandskAdresse,
                vegadresse = vegadresse,
                ukjentBosted = UkjentBosted(""),
                matrikkeladresse = matrikkeladresse,
                metadata = metadataGjeldende,
            ),
        )

    private val dødsfall = listOf(Dødsfall(LocalDate.now()))

    private val familierelasjon =
        listOf(ForelderBarnRelasjon("", Familierelasjonsrolle.BARN, Familierelasjonsrolle.FAR))

    private val fødselsdato = listOf(Fødselsdato(1, LocalDate.now(), metadataGjeldende))

    private val opphold = listOf(Opphold(Oppholdstillatelse.MIDLERTIDIG, LocalDate.now(), LocalDate.now()))

    private val oppholdsadresse =
        listOf(
            Oppholdsadresse(
                LocalDate.now(),
                null,
                "",
                utenlandskAdresse,
                vegadresse,
                "",
                metadataGjeldende,
            ),
        )

    private val statsborgerskap = listOf(Statsborgerskap("", LocalDate.now(), LocalDate.now()))

    private val innflyttingTilNorge = listOf(InnflyttingTilNorge("", "", folkeregistermetadata))

    private val utflyttingFraNorge = listOf(UtflyttingFraNorge("", "", LocalDate.now(), folkeregistermetadata))

    val søkerIdentifikator = "1"

    val folkeregisteridentifikatorSøker =
        listOf(
            Folkeregisteridentifikator(
                ident = søkerIdentifikator,
                status = FolkeregisteridentifikatorStatus.I_BRUK,
                metadata = metadataGjeldende,
            ),
        )

    val pdlSøkerData =
        PdlSøkerData(
            PdlSøker(
                adressebeskyttelse = adressebeskyttelse,
                bostedsadresse = bostedsadresse,
                dødsfall = dødsfall,
                forelderBarnRelasjon = familierelasjon,
                folkeregisteridentifikator = folkeregisteridentifikatorSøker,
                fødselsdato = fødselsdato,
                folkeregisterpersonstatus = listOf(Folkeregisterpersonstatus("", "", metadataGjeldende)),
                kontaktadresse =
                    listOf(
                        Kontaktadresse(
                            coAdressenavn = "",
                            gyldigFraOgMed = LocalDate.now(),
                            gyldigTilOgMed = LocalDate.now(),
                            postadresseIFrittFormat = PostadresseIFrittFormat("", "", "", ""),
                            postboksadresse = Postboksadresse("", "", ""),
                            type = KontaktadresseType.INNLAND,
                            utenlandskAdresse = utenlandskAdresse,
                            utenlandskAdresseIFrittFormat = UtenlandskAdresseIFrittFormat("", "", "", "", "", ""),
                            vegadresse = vegadresse,
                        ),
                    ),
                navn = navn,
                opphold = opphold,
                oppholdsadresse = oppholdsadresse,
                statsborgerskap = statsborgerskap,
                innflyttingTilNorge = innflyttingTilNorge,
                utflyttingFraNorge = utflyttingFraNorge,
                vergemaalEllerFremtidsfullmakt =
                    listOf(
                        VergemaalEllerFremtidsfullmakt(
                            embete = "",
                            folkeregistermetadata = folkeregistermetadata,
                            type = "",
                            vergeEllerFullmektig =
                                VergeEllerFullmektig(
                                    IdentifiserendeInformasjon(Personnavn("", "", "")),
                                    "",
                                    "",
                                ),
                        ),
                    ),
            ),
        )

    val pdlBarnData =
        PersonBolk(
            listOf(
                PersonDataBolk(
                    "11111122222",
                    "ok",
                    PdlBarn(
                        adressebeskyttelse,
                        bostedsadresse,
                        dødsfall,
                        familierelasjon,
                        fødselsdato,
                        navn,
                    ),
                ),
            ),
        )

    val ennenForelderIdentifikator = "2"

    val folkeregisteridentifikatorAnnenForelder =
        listOf(
            Folkeregisteridentifikator(
                ennenForelderIdentifikator,
                FolkeregisteridentifikatorStatus.I_BRUK,
                metadataGjeldende,
            ),
        )

    val pdlAnnenForelderData =
        PersonBolk(
            listOf(
                PersonDataBolk(
                    "11111122222",
                    "ok",
                    PdlAnnenForelder(
                        adressebeskyttelse,
                        bostedsadresse,
                        dødsfall,
                        fødselsdato,
                        folkeregisteridentifikatorAnnenForelder,
                        navn,
                    ),
                ),
            ),
        )

    val pdlPersonKortBolk =
        PersonBolk(
            listOf(
                PersonDataBolk(
                    "11111122222",
                    "ok",
                    PdlPersonKort(
                        adressebeskyttelse,
                        navn,
                        dødsfall,
                    ),
                ),
            ),
        )

    val pdlPersonSøk =
        PersonSøk(
            PersonSøkResultat(
                hits =
                    listOf(
                        PersonSøkTreff(
                            PdlPersonFraSøk(
                                listOf(FolkeregisteridentifikatorFraSøk("123456789")),
                                bostedsadresse,
                                navn,
                            ),
                        ),
                    ),
                totalHits = 1,
                pageNumber = 1,
                totalPages = 1,
            ),
        )
}
