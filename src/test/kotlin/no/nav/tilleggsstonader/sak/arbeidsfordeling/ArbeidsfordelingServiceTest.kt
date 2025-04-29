package no.nav.tilleggsstonader.sak.arbeidsfordeling

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.pdl.GeografiskTilknytningDto
import no.nav.tilleggsstonader.kontrakter.pdl.GeografiskTilknytningType
import no.nav.tilleggsstonader.sak.opplysninger.egenansatt.EgenAnsatt
import no.nav.tilleggsstonader.sak.opplysninger.egenansatt.EgenAnsattService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveUtil.ENHET_NR_NAY
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveUtil.ENHET_NR_STRENGT_FORTROLIG
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.domain.AdressebeskyttelseForPersonMedRelasjoner
import no.nav.tilleggsstonader.sak.opplysninger.pdl.domain.AdressebeskyttelseForPersonUtenRelasjoner
import no.nav.tilleggsstonader.sak.opplysninger.pdl.domain.PersonMedAdresseBeskyttelse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.tilDiskresjonskode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

class ArbeidsfordelingServiceTest {
    val cacheManager = ConcurrentMapCacheManager()
    val personService = mockk<PersonService>()
    val egenAnsattService = mockk<EgenAnsattService>()
    val arbeidsfordelingClient = mockk<ArbeidsfordelingClient>()

    val service =
        ArbeidsfordelingService(
            cacheManager = cacheManager,
            arbeidsfordelingClient = arbeidsfordelingClient,
            personService = personService,
            egenAnsattService = egenAnsattService,
        )

    val søkerIdent = "søker"
    val annenForeldreIdent = "annenForeldre"
    val barnIdent = "barnIdent"

    val kritierieSlot = slot<ArbeidsfordelingKriterie>()
    val slotEgenAnsatt = slot<Set<String>>()

    @BeforeEach
    fun setUp() {
        kritierieSlot.clear()
        slotEgenAnsatt.clear()

        every { arbeidsfordelingClient.finnArbeidsfordelingsenhet(capture(kritierieSlot)) } answers {
            val diskresjonskodeStrengtFortrolig = AdressebeskyttelseGradering.STRENGT_FORTROLIG.tilDiskresjonskode()
            val kriterie = firstArg<ArbeidsfordelingKriterie>()
            if (kriterie.diskresjonskode == diskresjonskodeStrengtFortrolig) {
                listOf(Arbeidsfordelingsenhet(ENHET_NR_STRENGT_FORTROLIG, "vikafossen"))
            } else {
                listOf(Arbeidsfordelingsenhet(ENHET_NR_NAY, "nay"))
            }
        }
        val geografiskTilknytning =
            GeografiskTilknytningDto(GeografiskTilknytningType.KOMMUNE, "kommune", "bydel", "land")
        every { personService.hentGeografiskTilknytning(søkerIdent) } returns geografiskTilknytning
        every { egenAnsattService.erEgenAnsatt(capture(slotEgenAnsatt)) } answers {
            firstArg<Set<String>>().associateWith { EgenAnsatt(it, false) }
        }
    }

    @AfterEach
    fun tearDown() {
        cacheManager.cacheNames.stream().forEach { cacheManager.getCache(it)?.clear() }
    }

    @Nested
    inner class Adressebeskyttelse {
        @Test
        fun `skal bruke adressebeskyttelse til søker hvis den er den høyeste`() {
            every { personService.hentAdressebeskyttelseForPersonOgRelasjoner(søkerIdent) } returns
                lagPersonMedRelasjoner(graderingSøker = AdressebeskyttelseGradering.STRENGT_FORTROLIG)

            service.hentNavEnhet(søkerIdent, Stønadstype.BARNETILSYN)

            assertThat(kritierieSlot.captured.diskresjonskode).isEqualTo("SPSF")
        }

        @Test
        fun `skal bruke adressebeskyttelse til barn hvis den er den høyeste`() {
            every { personService.hentAdressebeskyttelseForPersonOgRelasjoner(søkerIdent) } returns
                lagPersonMedRelasjoner(graderingBarn = AdressebeskyttelseGradering.FORTROLIG)

            service.hentNavEnhet(søkerIdent, Stønadstype.BARNETILSYN)

            assertThat(kritierieSlot.captured.diskresjonskode).isEqualTo("SPFO")
        }

        @Test
        fun `skal bruke adressebeskyttelse til annen foreldre hvis den er den høyeste`() {
            every { personService.hentAdressebeskyttelseForPersonOgRelasjoner(søkerIdent) } returns
                lagPersonMedRelasjoner(graderingBarn = AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND)

            service.hentNavEnhet(søkerIdent, Stønadstype.BARNETILSYN)

            assertThat(kritierieSlot.captured.diskresjonskode).isEqualTo("SPSF")
        }

        @Test
        fun `skal hente adressebeskyttelse for barn og andre foreldre for tilsyn barn då de er parter på saken`() {
            every { personService.hentAdressebeskyttelseForPersonOgRelasjoner(søkerIdent) } returns
                lagPersonMedRelasjoner()

            service.hentNavEnhet(søkerIdent, Stønadstype.BARNETILSYN)

            verify(exactly = 1) { personService.hentAdressebeskyttelseForPersonOgRelasjoner(any()) }
            verify(exactly = 0) { personService.hentAdressebeskyttelse(any()) }
        }

        @Test
        fun `skal kun hente adressebeskyttelse for søker hvis stønaden ikke gjelder barn`() {
            every { personService.hentAdressebeskyttelse(søkerIdent) } returns lagPersonUtenRelasjoner()

            service.hentNavEnhet(søkerIdent, Stønadstype.LÆREMIDLER)

            verify(exactly = 0) { personService.hentAdressebeskyttelseForPersonOgRelasjoner(any()) }
            verify(exactly = 1) { personService.hentAdressebeskyttelse(any()) }
        }
    }

    @Nested
    inner class HentNavEnhetCache {
        @Test
        fun `skal cachea svar fra arbeidsfordeling`() {
            every { personService.hentAdressebeskyttelse(søkerIdent) } returns lagPersonUtenRelasjoner()

            service.hentNavEnhet(søkerIdent, Stønadstype.LÆREMIDLER)
            service.hentNavEnhet(søkerIdent, Stønadstype.LÆREMIDLER)

            verify(exactly = 1) { arbeidsfordelingClient.finnArbeidsfordelingsenhet(any()) }
        }

        @Test
        fun `skal ikke bruke cache for ny stønadstype`() {
            every { personService.hentAdressebeskyttelseForPersonOgRelasjoner(søkerIdent) } returns
                lagPersonMedRelasjoner(graderingBarn = AdressebeskyttelseGradering.STRENGT_FORTROLIG)
            every { personService.hentAdressebeskyttelse(søkerIdent) } returns lagPersonUtenRelasjoner()

            val arbeidsfordelingTilsynBarn = service.hentNavEnhet(søkerIdent, Stønadstype.BARNETILSYN)
            val arbeidsfordelingLæremidler = service.hentNavEnhet(søkerIdent, Stønadstype.LÆREMIDLER)

            assertThat(arbeidsfordelingTilsynBarn?.enhetNr).isEqualTo(ENHET_NR_STRENGT_FORTROLIG)
            assertThat(arbeidsfordelingLæremidler?.enhetNr).isEqualTo(ENHET_NR_NAY)

            verify(exactly = 2) { arbeidsfordelingClient.finnArbeidsfordelingsenhet(any()) }
        }
    }

    @Nested
    inner class EgenAnsatt {
        @Test
        fun `skal kontrollere egenAnsatt til bruker og annen foreldre`() {
            every { personService.hentAdressebeskyttelseForPersonOgRelasjoner(søkerIdent) } returns
                lagPersonMedRelasjoner()

            service.hentNavEnhet(søkerIdent, Stønadstype.BARNETILSYN)

            assertThat(slotEgenAnsatt.captured)
                .containsExactlyInAnyOrder(søkerIdent, annenForeldreIdent)
        }
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

    private fun lagPersonUtenRelasjoner(
        graderingSøker: AdressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
    ): AdressebeskyttelseForPersonUtenRelasjoner =
        AdressebeskyttelseForPersonUtenRelasjoner(
            søker = PersonMedAdresseBeskyttelse(søkerIdent, graderingSøker),
        )
}
