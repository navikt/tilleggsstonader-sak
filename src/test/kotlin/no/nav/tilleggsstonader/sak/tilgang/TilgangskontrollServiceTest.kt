package no.nav.tilleggsstonader.sak.tilgang

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.RolleConfig
import no.nav.tilleggsstonader.sak.opplysninger.egenansatt.EgenAnsatt
import no.nav.tilleggsstonader.sak.opplysninger.egenansatt.EgenAnsattService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.domain.AdressebeskyttelseForPersonMedRelasjoner
import no.nav.tilleggsstonader.sak.opplysninger.pdl.domain.AdressebeskyttelseForPersonUtenRelasjoner
import no.nav.tilleggsstonader.sak.opplysninger.pdl.domain.PersonMedAdresseBeskyttelse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class TilgangskontrollServiceTest {
    private val egenAnsattService = mockk<EgenAnsattService>()
    private val personService = mockk<PersonService>()
    private val kode7Id = "6"
    private val kode6Id = "7"
    private val egenAnsattId = "egenANsatt"

    private val søkerIdent = "søker"
    private val annenForeldreIdent = "annenForeldreIdent"
    private val barnIdent = "barn1"

    private val tilgangConfig =
        RolleConfig(
            veilederRolle = "",
            saksbehandlerRolle = "",
            beslutterRolle = "",
            kode7 = kode7Id,
            kode6 = kode6Id,
            egenAnsatt = egenAnsattId,
            prosessering = "",
        )

    private val tilgangskontrollService =
        TilgangskontrollService(
            egenAnsattService = egenAnsattService,
            personService = personService,
            rolleConfig = tilgangConfig,
        )

    private val jwtToken = mockk<JwtToken>(relaxed = true)
    private val jwtTokenClaims = mockk<JwtTokenClaims>()

    val slotEgenAnsatt = slot<Set<String>>()

    @BeforeEach
    internal fun setUp() {
        every { jwtToken.jwtTokenClaims } returns jwtTokenClaims
        every { jwtTokenClaims.get("preferred_username") }.returns(listOf("bob"))
        every { jwtTokenClaims.getAsList(any()) }.returns(emptyList())
        mockHentAdressebeskyttelse(AdressebeskyttelseGradering.UGRADERT)
        every { egenAnsattService.erEgenAnsatt(any<String>()) } returns false
        every { egenAnsattService.erEgenAnsatt(capture(slotEgenAnsatt)) } answers
            { firstArg<Set<String>>().associateWith { EgenAnsatt(it, false) } }
        slotEgenAnsatt.clear()
    }

    @Nested
    inner class SjekkTilgangTilPerson {
        @Test
        internal fun `har tilgang når det ikke finnes noen adressebeskyttelser for enskild person`() {
            assertThat(sjekkTilgangTilPerson()).isTrue
            verify(exactly = 1) { egenAnsattService.erEgenAnsatt(any<String>()) }
        }

        @Test
        internal fun `har ikke tilgang når det finnes adressebeskyttelser for enskild person`() {
            mockHentAdressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG)
            assertThat(sjekkTilgangTilPerson()).isFalse
            verify(exactly = 0) { egenAnsattService.erEgenAnsatt(any<String>()) }
        }

        @Test
        internal fun `har ikke tilgang når det ikke finnes noen adressebeskyttelser for enskild person men er ansatt`() {
            every { egenAnsattService.erEgenAnsatt(any<String>()) } returns true
            assertThat(sjekkTilgangTilPerson()).isFalse
            verify(exactly = 1) { egenAnsattService.erEgenAnsatt(any<String>()) }
        }

        private fun sjekkTilgangTilPerson() = tilgangskontrollService.sjekkTilgang(søkerIdent, jwtToken).harTilgang
    }

    @Nested
    inner class SjekkTilgangTilStønadstype {
        @Test
        fun `skal hente adressebeskyttelse for person med barn hvis det gjelder stønad som gjelder barn`() {
            every { personService.hentAdressebeskyttelseForPersonOgRelasjoner(any()) } returns lagPersonMedRelasjoner()

            tilgangskontrollService.sjekkTilgangTilStønadstype(søkerIdent, Stønadstype.BARNETILSYN, jwtToken)

            verify(exactly = 1) { personService.hentAdressebeskyttelseForPersonOgRelasjoner(søkerIdent) }
            verify(exactly = 0) { personService.hentAdressebeskyttelse(any()) }
        }

        @Test
        fun `skal kun hente adressebeskyttelse for søker hvis det gjelder stønad som ikke gjelder barn`() {
            every { personService.hentAdressebeskyttelse(any()) } returns lagAdressebeskyttelse()

            tilgangskontrollService.sjekkTilgangTilStønadstype(søkerIdent, Stønadstype.LÆREMIDLER, jwtToken)

            verify(exactly = 0) { personService.hentAdressebeskyttelseForPersonOgRelasjoner(any()) }
            verify(exactly = 1) { personService.hentAdressebeskyttelse(søkerIdent) }
        }
    }

    @Nested
    inner class SjekkTilgangTilStønadstypeMedBarn {
        @Test
        fun `skal hente ut barn koblet til fagsakPerson og kontrollere barn og andre foreldre`() {
            every { personService.hentAdressebeskyttelseForPersonOgRelasjoner(any()) } returns lagPersonMedRelasjoner()

            val tilgang = sjekkTilgangTilTilsynBarn()

            assertThat(tilgang).isTrue
            assertThat(slotEgenAnsatt.captured).containsExactlyInAnyOrder(søkerIdent, annenForeldreIdent)
        }

        @Test
        internal fun `har tilgang når det ikke finnes noen adressebeskyttelser`() {
            every { personService.hentAdressebeskyttelseForPersonOgRelasjoner(any()) } returns lagPersonMedRelasjoner()
            assertThat(sjekkTilgangTilTilsynBarn()).isTrue
        }

        @Test
        internal fun `har ikke tilgang når søkeren er STRENGT_FORTROLIG og saksbehandler har kode7`() {
            mockHarKode7()
            every { personService.hentAdressebeskyttelseForPersonOgRelasjoner(any()) } returns
                lagPersonMedRelasjoner(AdressebeskyttelseGradering.STRENGT_FORTROLIG)
            assertThat(sjekkTilgangTilTilsynBarn()).isFalse
        }

        @Test
        internal fun `har tilgang når søkeren er STRENGT_FORTROLIG og saksbehandler har kode6`() {
            mockHarKode6()
            every { personService.hentAdressebeskyttelseForPersonOgRelasjoner(any()) } returns
                lagPersonMedRelasjoner(AdressebeskyttelseGradering.STRENGT_FORTROLIG)
            assertThat(sjekkTilgangTilTilsynBarn()).isTrue
        }

        @Test
        internal fun `har ikke tilgang når det finnes adressebeskyttelse for søkeren`() {
            every { personService.hentAdressebeskyttelseForPersonOgRelasjoner(any()) } returns
                lagPersonMedRelasjoner(graderingSøker = AdressebeskyttelseGradering.FORTROLIG)
            assertThat(sjekkTilgangTilTilsynBarn()).isFalse
        }

        @Test
        internal fun `har ikke tilgang når barn inneholder FORTROLIG`() {
            every { personService.hentAdressebeskyttelseForPersonOgRelasjoner(any()) } returns
                lagPersonMedRelasjoner(graderingBarn = AdressebeskyttelseGradering.FORTROLIG)
            assertThat(sjekkTilgangTilTilsynBarn()).isFalse
        }

        @Test
        internal fun `har ikke tilgang når søker er egenansatt`() {
            every { personService.hentAdressebeskyttelseForPersonOgRelasjoner(any()) } returns lagPersonMedRelasjoner()
            every { egenAnsattService.erEgenAnsatt(any<Set<String>>()) } answers {
                firstArg<Set<String>>().associateWith { EgenAnsatt(it, it == søkerIdent) }
            }
            assertThat(sjekkTilgangTilTilsynBarn()).isFalse
        }

        @Test
        fun `skal ikke kontrollere egenansatt på barn`() {
            every { personService.hentAdressebeskyttelseForPersonOgRelasjoner(any()) } returns lagPersonMedRelasjoner()
            val slot = slot<Set<String>>()
            every { egenAnsattService.erEgenAnsatt(capture(slot)) } answers {
                firstArg<Set<String>>().associateWith { EgenAnsatt(it, false) }
            }
            sjekkTilgangTilTilsynBarn()
            assertThat(slot.captured).containsOnly(søkerIdent, annenForeldreIdent)
        }

        private fun sjekkTilgangTilTilsynBarn() =
            tilgangskontrollService.sjekkTilgangTilStønadstype(søkerIdent, Stønadstype.BARNETILSYN, jwtToken).harTilgang
    }

    private fun mockHarKode7() {
        every { jwtTokenClaims.getAsList(any()) }.returns(listOf(kode7Id))
    }

    private fun mockHarKode6() {
        every { jwtTokenClaims.getAsList(any()) }.returns(listOf(kode6Id))
    }

    private fun lagPersonMedRelasjoner(
        graderingSøker: AdressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
        graderingBarn: AdressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
        graderingAnnenForelder: AdressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
    ): AdressebeskyttelseForPersonMedRelasjoner =
        AdressebeskyttelseForPersonMedRelasjoner(
            søker = PersonMedAdresseBeskyttelse(søkerIdent, graderingSøker),
            barn = listOf(PersonMedAdresseBeskyttelse(barnIdent, graderingBarn)),
            andreForeldre = listOf(PersonMedAdresseBeskyttelse(annenForeldreIdent, graderingAnnenForelder)),
        )

    private fun lagAdressebeskyttelse(
        graderingSøker: AdressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
    ): AdressebeskyttelseForPersonUtenRelasjoner =
        AdressebeskyttelseForPersonUtenRelasjoner(
            søker = PersonMedAdresseBeskyttelse(søkerIdent, graderingSøker),
        )

    private fun mockHentAdressebeskyttelse(
        gradering: AdressebeskyttelseGradering,
        ident: String = søkerIdent,
    ) {
        val søker = PersonMedAdresseBeskyttelse(ident, gradering)
        every { personService.hentAdressebeskyttelse(ident) } returns AdressebeskyttelseForPersonUtenRelasjoner(søker)
    }
}
