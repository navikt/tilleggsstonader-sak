package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.tilleggsstonader.kontrakter.pdl.GeografiskTilknytningDto
import no.nav.tilleggsstonader.kontrakter.pdl.GeografiskTilknytningType
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PdlClient
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PdlNotFoundException
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Adressebeskyttelse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Bostedsadresse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Dødsfall
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Familierelasjonsrolle
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Folkeregisteridentifikator
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.FolkeregisteridentifikatorStatus
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Folkeregistermetadata
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Folkeregisterpersonstatus
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.ForelderBarnRelasjon
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.IdentifiserendeInformasjon
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.InnflyttingTilNorge
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Kontaktadresse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.KontaktadresseType
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Navn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Opphold
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Oppholdstillatelse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlAnnenForelder
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlBarn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdenter
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlPersonKort
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Statsborgerskap
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.UtflyttingFraNorge
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Vegadresse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.VergeEllerFullmektig
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.VergemaalEllerFremtidsfullmakt
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.fødsel
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.lagNavn
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.metadataGjeldende
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.pdlBarn
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.pdlSøker
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

@Configuration
@Profile("mock-pdl")
class PdlClientConfig {
    @Bean
    @Primary
    fun pdlClient(): PdlClient {
        val pdlClient: PdlClient = mockk()
        resetMock(pdlClient)
        return pdlClient
    }

    companion object {
        private val startdato = LocalDate.of(2020, 1, 1)
        private val sluttdato = LocalDate.of(2021, 1, 1)
        const val BARN_FNR = "01012067050"
        const val BARN2_FNR = "14041385481"
        const val SØKER_FNR = "01010172272"
        const val ANNEN_FORELDER_FNR = "17097926735"

        fun resetMock(pdlClient: PdlClient) {
            clearMocks(pdlClient)

            every { pdlClient.hentPersonKortBolk(any()) } answers {
                firstArg<List<String>>().associateWith { lagPersonKort(it) }
            }

            every { pdlClient.hentSøker(any()) } answers {
                opprettPdlSøker()
            }

            every { pdlClient.hentBarn(any()) } returns barn()

            every { pdlClient.hentAndreForeldre(any()) } returns mapOf(ANNEN_FORELDER_FNR to annenForelder())

            val personIdent = slot<String>()
            every { pdlClient.hentPersonidenter(capture(personIdent)) } answers {
                val capturedIdent = personIdent.captured
                if (personIdent.captured == "19117313797") {
                    throw PdlNotFoundException()
                } else {
                    PdlIdenter(
                        identer =
                            listOf(
                                PdlIdent(capturedIdent, false, "FOLKEREGISTERIDENT"),
                                PdlIdent("98765432109", true, "FOLKEREGISTERIDENT"),
                                PdlIdent("00$capturedIdent", false, "AKTORID"),
                            ),
                    )
                }
            }

            every { pdlClient.hentIdenterBolk(listOf("123", "456")) }
                .returns(
                    mapOf(
                        "123" to PdlIdent("ny123", false, "FOLKEREGISTERIDENT"),
                        "456" to PdlIdent("ny456", false, "FOLKEREGISTERIDENT"),
                    ),
                )

            every { pdlClient.hentIdenterBolk(listOf("456", "123")) }
                .returns(
                    mapOf(
                        "123" to PdlIdent("ny123", false, "FOLKEREGISTERIDENT"),
                        "456" to PdlIdent("ny456", false, "FOLKEREGISTERIDENT"),
                    ),
                )

            every { pdlClient.hentIdenterBolk(listOf("111", "222")) }
                .returns(
                    mapOf(
                        "111" to PdlIdent("111", false, "FOLKEREGISTERIDENT"),
                        "222" to PdlIdent("222", false, "FOLKEREGISTERIDENT"),
                    ),
                )

            every { pdlClient.hentGeografiskTilknytning(any()) } returns
                GeografiskTilknytningDto(
                    gtBydel = "030103",
                    gtKommune = "0301",
                    gtType = GeografiskTilknytningType.BYDEL,
                    gtLand = "NOR",
                )
        }

        fun lagPersonKort(it: String) =
            PdlPersonKort(
                listOf(
                    Adressebeskyttelse(
                        gradering = AdressebeskyttelseGradering.UGRADERT,
                        metadata = metadataGjeldende,
                    ),
                ),
                listOf(lagNavn(fornavn = it)),
                emptyList(),
            )

        val folkeregisteridentifikatorSøker =
            Folkeregisteridentifikator(
                SØKER_FNR,
                FolkeregisteridentifikatorStatus.I_BRUK,
                metadataGjeldende,
            )

        fun opprettPdlSøker() =
            pdlSøker(
                adressebeskyttelse =
                    listOf(
                        Adressebeskyttelse(
                            gradering = AdressebeskyttelseGradering.UGRADERT,
                            metadata = metadataGjeldende,
                        ),
                    ),
                bostedsadresse = bostedsadresse(),
                dødsfall = listOf(),
                forelderBarnRelasjon = forelderBarnRelasjoner(),
                folkeregisteridentifikator = listOf(folkeregisteridentifikatorSøker),
                fødselsdato = listOf(fødsel()),
                folkeregisterpersonstatus =
                    listOf(
                        Folkeregisterpersonstatus(
                            "bosatt",
                            "bosattEtterFolkeregisterloven",
                            metadataGjeldende,
                        ),
                    ),
                kontaktadresse = kontaktadresse(),
                navn = listOf(lagNavn()),
                opphold = listOf(Opphold(Oppholdstillatelse.PERMANENT, startdato, null)),
                oppholdsadresse = listOf(),
                statsborgerskap = statsborgerskap(),
                innflyttingTilNorge = listOf(InnflyttingTilNorge("SWE", "Stockholm", folkeregistermetadata)),
                utflyttingFraNorge =
                    listOf(
                        UtflyttingFraNorge(
                            tilflyttingsland = "SWE",
                            tilflyttingsstedIUtlandet = "Stockholm",
                            utflyttingsdato = LocalDate.of(2021, 1, 1),
                            folkeregistermetadata = folkeregistermetadata,
                        ),
                    ),
                vergemaalEllerFremtidsfullmakt = vergemaalEllerFremtidsfullmakt(),
            )

        private val folkeregistermetadata =
            Folkeregistermetadata(
                LocalDateTime.of(2010, Month.AUGUST, 30, 10, 10),
                LocalDateTime.of(2018, Month.JANUARY, 15, 12, 55),
            )

        private fun barn(): Map<String, PdlBarn> =
            mapOf(
                BARN_FNR to
                    pdlBarn(
                        bostedsadresse = bostedsadresse(),
                        forelderBarnRelasjon = familierelasjonerBarn(),
                        fødselsdato = fødsel(),
                        navn = lagNavn("Barn", null, "Barnesen"),
                    ),
                BARN2_FNR to
                    pdlBarn(
                        bostedsadresse = bostedsadresse(),
                        forelderBarnRelasjon = familierelasjonerBarn(),
                        fødselsdato = fødsel(),
                        navn = lagNavn("Barn2", null, "Barnesen"),
                    ),
            )

        private fun annenForelder(): PdlAnnenForelder =
            PdlAnnenForelder(
                adressebeskyttelse = emptyList(),
                bostedsadresse = bostedsadresse(),
                dødsfall = listOf(Dødsfall(LocalDate.of(2021, 9, 22))),
                fødselsdato = listOf(fødsel(1994, 11, 1)),
                navn = listOf(Navn("Bob", "", "Burger", metadataGjeldende)),
                folkeregisteridentifikator =
                    listOf(
                        Folkeregisteridentifikator(
                            ANNEN_FORELDER_FNR,
                            FolkeregisteridentifikatorStatus.I_BRUK,
                            metadataGjeldende,
                        ),
                    ),
            )

        private fun forelderBarnRelasjoner(): List<ForelderBarnRelasjon> =
            listOf(
                ForelderBarnRelasjon(
                    relatertPersonsIdent = BARN_FNR,
                    relatertPersonsRolle = Familierelasjonsrolle.BARN,
                    minRolleForPerson = Familierelasjonsrolle.MOR,
                ),
                ForelderBarnRelasjon(
                    relatertPersonsIdent = BARN2_FNR,
                    relatertPersonsRolle = Familierelasjonsrolle.BARN,
                    minRolleForPerson = Familierelasjonsrolle.MOR,
                ),
            )

        fun familierelasjonerBarn(): List<ForelderBarnRelasjon> =
            listOf(
                ForelderBarnRelasjon(
                    relatertPersonsIdent = SØKER_FNR,
                    relatertPersonsRolle = Familierelasjonsrolle.MOR,
                    minRolleForPerson = Familierelasjonsrolle.BARN,
                ),
                ForelderBarnRelasjon(
                    relatertPersonsIdent = ANNEN_FORELDER_FNR,
                    relatertPersonsRolle = Familierelasjonsrolle.FAR,
                    minRolleForPerson = Familierelasjonsrolle.BARN,
                ),
            )

        private fun kontaktadresse(): List<Kontaktadresse> =
            listOf(
                Kontaktadresse(
                    coAdressenavn = "co",
                    gyldigFraOgMed = startdato,
                    gyldigTilOgMed = sluttdato,
                    postadresseIFrittFormat = null,
                    postboksadresse = null,
                    type = KontaktadresseType.INNLAND,
                    utenlandskAdresse = null,
                    utenlandskAdresseIFrittFormat = null,
                    vegadresse = vegadresse(),
                ),
            )

        private fun statsborgerskap(): List<Statsborgerskap> =
            listOf(
                Statsborgerskap(
                    land = "NOR",
                    gyldigFraOgMed = startdato,
                    gyldigTilOgMed = null,
                ),
                Statsborgerskap(
                    land = "SWE",
                    gyldigFraOgMed = startdato.minusYears(3),
                    gyldigTilOgMed = startdato,
                ),
            )

        private fun bostedsadresse(): List<Bostedsadresse> =
            listOf(
                Bostedsadresse(
                    angittFlyttedato = startdato.plusDays(1),
                    gyldigFraOgMed = startdato,
                    gyldigTilOgMed = LocalDate.of(2199, 1, 1),
                    utenlandskAdresse = null,
                    coAdressenavn = "CONAVN",
                    vegadresse = vegadresse(),
                    ukjentBosted = null,
                    matrikkeladresse = null,
                    metadata = metadataGjeldende,
                ),
            )

        private fun vegadresse(): Vegadresse =
            Vegadresse(
                husnummer = "13",
                husbokstav = "b",
                adressenavn = "Charlies vei",
                kommunenummer = "0301",
                postnummer = "0575",
                bruksenhetsnummer = "",
                tilleggsnavn = null,
                matrikkelId = 0,
            )

        private fun vergemaalEllerFremtidsfullmakt(): List<VergemaalEllerFremtidsfullmakt> =
            listOf(
                VergemaalEllerFremtidsfullmakt(
                    embete = null,
                    folkeregistermetadata = null,
                    type = "voksen",
                    vergeEllerFullmektig =
                        VergeEllerFullmektig(
                            motpartsPersonident = ANNEN_FORELDER_FNR,
                            identifiserendeInformasjon = IdentifiserendeInformasjon(navn = null),
                            omfang = "personligeOgOekonomiskeInteresser",
                        ),
                ),
                VergemaalEllerFremtidsfullmakt(
                    embete = null,
                    folkeregistermetadata = null,
                    type = "stadfestetFremtidsfullmakt",
                    vergeEllerFullmektig =
                        VergeEllerFullmektig(
                            motpartsPersonident = ANNEN_FORELDER_FNR,
                            identifiserendeInformasjon = IdentifiserendeInformasjon(navn = null),
                            omfang = "personligeOgOekonomiskeInteresser",
                        ),
                ),
            )
    }
}
