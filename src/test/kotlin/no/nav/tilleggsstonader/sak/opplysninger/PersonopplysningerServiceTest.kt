package no.nav.tilleggsstonader.sak.opplysninger

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.opplysninger.dto.Adressebeskyttelse
import no.nav.tilleggsstonader.sak.opplysninger.egenansatt.EgenAnsattService
import no.nav.tilleggsstonader.sak.opplysninger.fullmakt.FullmaktService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering
import no.nav.tilleggsstonader.sak.util.FullmektigStubs
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.metadataGjeldende
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.pdlSøker
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.vergemaalEllerFremtidsfullmakt
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Adressebeskyttelse as AdressebeskyttelsePdl

class PersonopplysningerServiceTest {
    private val fagsakPersonService = mockk<FagsakPersonService>()
    private val behandlingService = mockk<BehandlingService>()
    private val personService = mockk<PersonService>()
    private val fullmaktService = mockk<FullmaktService>()
    private val egenAnsattService = mockk<EgenAnsattService>()

    private val service =
        PersonopplysningerService(
            fagsakPersonService = fagsakPersonService,
            behandlingService = behandlingService,
            personService = personService,
            fullmaktService = fullmaktService,
            egenAnsattService = egenAnsattService,
        )

    @BeforeEach
    fun setUp() {
        every { fagsakPersonService.hentAktivIdent(any()) } returns "0"
        every { behandlingService.hentAktivIdent(any()) } returns "1"
        every { personService.hentSøker(any()) } returns pdlSøker()
        every { fullmaktService.hentFullmektige(any()) } returns emptyList()
        every { egenAnsattService.erEgenAnsatt(any<String>()) } returns true
    }

    @Nested
    inner class HarVerge {
        @Test
        fun `har ikke verge hvis man ikke har noen vergemål`() {
            every { personService.hentSøker(any()) } returns pdlSøker()

            assertThat(service.hentPersonopplysningerForFagsakPerson(FagsakPersonId.random()).harVergemål).isFalse
        }

        @Test
        fun `har ikke verge hvis man kun har fremtidsfullmakt`() {
            val vergemaalEllerFremtidsfullmakt = vergemaalEllerFremtidsfullmakt(type = "stadfestetFremtidsfullmakt")
            val pdlSøker = pdlSøker(vergemaalEllerFremtidsfullmakt = listOf(vergemaalEllerFremtidsfullmakt))
            every { personService.hentSøker(any()) } returns pdlSøker

            assertThat(service.hentPersonopplysningerForFagsakPerson(FagsakPersonId.random()).harVergemål).isFalse
        }

        @Test
        fun `har verge hvis man har et vergemål`() {
            val pdlSøker = pdlSøker(vergemaalEllerFremtidsfullmakt = listOf(vergemaalEllerFremtidsfullmakt()))
            every { personService.hentSøker(any()) } returns pdlSøker

            assertThat(service.hentPersonopplysningerForFagsakPerson(FagsakPersonId.random()).harVergemål).isTrue
        }

        @Test
        fun `har fullmektig hvis det finnes gyldige fullmakter`() {
            every { fullmaktService.hentFullmektige(any()) } returns listOf(FullmektigStubs.gyldig)
            assertThat(service.hentPersonopplysningerForFagsakPerson(FagsakPersonId.random()).harFullmektig).isTrue
        }

        @Test
        fun `har ikke fullmektig hvis lista med fullmektige er tom`() {
            assertThat(service.hentPersonopplysningerForFagsakPerson(FagsakPersonId.random()).harFullmektig).isFalse
        }
    }

    @Nested
    inner class AdressebeskyttelseMapping {
        @Test
        fun `skal mappe status fra pdl til status`() {
            val gradering = AdressebeskyttelseGradering.FORTROLIG
            val pdlSøker = pdlSøker(adressebeskyttelse = listOf(AdressebeskyttelsePdl(gradering, metadataGjeldende)))
            every { personService.hentSøker(any()) } returns pdlSøker

            assertThat(service.hentPersonopplysningerForFagsakPerson(FagsakPersonId.random()).adressebeskyttelse)
                .isEqualTo(Adressebeskyttelse.FORTROLIG)
        }
    }
}
