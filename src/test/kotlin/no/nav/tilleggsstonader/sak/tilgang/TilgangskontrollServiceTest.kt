package no.nav.tilleggsstonader.sak.tilgang

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.RolleConfig
import no.nav.tilleggsstonader.sak.opplysninger.dto.SøkerMedBarn
import no.nav.tilleggsstonader.sak.opplysninger.egenansatt.EgenAnsattService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Adressebeskyttelse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.metadataGjeldende
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.pdlBarn
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.pdlSøker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TilgangskontrollServiceTest {

    private val egenAnsattService = mockk<EgenAnsattService>()
    private val personService = mockk<PersonService>()
    private val kode7Id = "6"
    private val kode6Id = "7"
    private val egenAnsattId = "egenANsatt"

    private val søkerIdent = "søker"
    private val barnIdent = "barn1"

    private val tilgangConfig = RolleConfig(
        veilederRolle = "",
        saksbehandlerRolle = "",
        beslutterRolle = "",
        kode7 = kode7Id,
        kode6 = kode6Id,
        egenAnsatt = egenAnsattId,
    )
    private val tilgangskontrollService = TilgangskontrollService(
        egenAnsattService,
        personService,
        tilgangConfig,
    )

    private val jwtToken = mockk<JwtToken>(relaxed = true)
    private val jwtTokenClaims = mockk<JwtTokenClaims>()

    @BeforeEach
    internal fun setUp() {
        every { jwtToken.jwtTokenClaims } returns jwtTokenClaims
        every { jwtTokenClaims.get("preferred_username") }.returns(listOf("bob"))
        every { jwtTokenClaims.getAsList(any()) }.returns(emptyList())
        every { egenAnsattService.erEgenAnsatt(any<String>()) } returns false
        every { egenAnsattService.erEgenAnsatt(any<Set<String>>()) } answers
            { firstArg<Set<String>>().associateWith { false } }
    }

    private fun mockHarKode7() {
        every { jwtTokenClaims.getAsList(any()) }.returns(listOf(kode7Id))
    }

    private fun mockHarKode6() {
        every { jwtTokenClaims.getAsList(any()) }.returns(listOf(kode6Id))
    }

    @Test
    internal fun `har tilgang når det ikke finnes noen adressebeskyttelser`() {
        every { personService.hentPersonMedBarn(any()) } returns lagPersonMedRelasjoner()
        assertThat(sjekkTilgangTilPersonMedRelasjoner()).isTrue
    }

    @Test
    internal fun `har ikke tilgang når søkeren er STRENGT_FORTROLIG og saksbehandler har kode7`() {
        mockHarKode7()
        every { personService.hentPersonMedBarn(any()) } returns
            lagPersonMedRelasjoner(AdressebeskyttelseGradering.STRENGT_FORTROLIG)
        assertThat(sjekkTilgangTilPersonMedRelasjoner()).isFalse
    }

    @Test
    internal fun `har tilgang når søkeren er STRENGT_FORTROLIG og saksbehandler har kode6`() {
        mockHarKode6()
        every { personService.hentPersonMedBarn(any()) } returns
            lagPersonMedRelasjoner(AdressebeskyttelseGradering.STRENGT_FORTROLIG)
        assertThat(sjekkTilgangTilPersonMedRelasjoner()).isTrue
    }

    @Test
    internal fun `har ikke tilgang når det finnes adressebeskyttelse for søkeren`() {
        every { personService.hentPersonMedBarn(any()) } returns
            lagPersonMedRelasjoner(graderingSøker = AdressebeskyttelseGradering.FORTROLIG)
        assertThat(sjekkTilgangTilPersonMedRelasjoner()).isFalse
    }

    @Test
    internal fun `har ikke tilgang når barn inneholder FORTROLIG`() {
        every { personService.hentPersonMedBarn(any()) } returns
            lagPersonMedRelasjoner(graderingBarn = AdressebeskyttelseGradering.FORTROLIG)
        assertThat(sjekkTilgangTilPersonMedRelasjoner()).isFalse
    }

    @Test
    internal fun `har ikke tilgang når søker er egenansatt`() {
        every { personService.hentPersonMedBarn(any()) } returns lagPersonMedRelasjoner()
        every { egenAnsattService.erEgenAnsatt(any<Set<String>>()) } answers {
            firstArg<Set<String>>().associateWith { it == søkerIdent }
        }
        assertThat(sjekkTilgangTilPersonMedRelasjoner()).isFalse
    }

    @Test
    fun `skal ikke kontrollere egenansatt på barn`() {
        every { personService.hentPersonMedBarn(any()) } returns lagPersonMedRelasjoner()
        val slot = slot<Set<String>>()
        every { egenAnsattService.erEgenAnsatt(capture(slot)) } answers {
            firstArg<Set<String>>().associateWith { false }
        }
        sjekkTilgangTilPersonMedRelasjoner()
        assertThat(slot.captured).containsOnly(søkerIdent)
    }

    private fun sjekkTilgangTilPersonMedRelasjoner() =
        tilgangskontrollService.sjekkTilgangTilPersonMedRelasjoner("", jwtToken).harTilgang

    private fun lagPersonMedRelasjoner(
        graderingSøker: AdressebeskyttelseGradering? = null,
        graderingBarn: AdressebeskyttelseGradering? = null,
    ): SøkerMedBarn {
        val barnliste = mapOf(
            barnIdent to pdlBarn(
                adressebeskyttelse = graderingBarn?.let {
                    listOf(Adressebeskyttelse(it, metadataGjeldende))
                } ?: emptyList(),
            ),
        )
        val søker = pdlSøker(
            adressebeskyttelse = graderingSøker?.let { listOf(Adressebeskyttelse(it, metadataGjeldende)) }
                ?: emptyList(),
        )
        return SøkerMedBarn(
            søkerIdent = søkerIdent,
            søker = søker,
            barn = barnliste,
        )
    }
}
