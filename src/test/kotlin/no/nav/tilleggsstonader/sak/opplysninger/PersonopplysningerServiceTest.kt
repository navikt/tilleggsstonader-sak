package no.nav.tilleggsstonader.sak.opplysninger

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.pdlSøker
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.vergemaalEllerFremtidsfullmakt
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class PersonopplysningerServiceTest {

    private val fagsakPersonService = mockk<FagsakPersonService>()
    private val behandlingService = mockk<BehandlingService>()
    private val personService = mockk<PersonService>()

    private val service = PersonopplysningerService(
        fagsakPersonService = fagsakPersonService,
        behandlingService = behandlingService,
        personService = personService,
    )

    @BeforeEach
    fun setUp() {
        every { fagsakPersonService.hentAktivIdent(any()) } returns "0"
        every { behandlingService.hentAktivIdent(any()) } returns "1"
        every { personService.hentSøker(any()) } returns pdlSøker()
    }

    @Nested
    inner class HarVerge {
        @Test
        fun `har ikke verge hvis man ikke har noen vergemål`() {
            every { personService.hentSøker(any()) } returns pdlSøker()

            assertThat(service.hentPersonopplysningerForFagsakPerson(UUID.randomUUID()).harVergemål).isFalse
        }

        @Test
        fun `har ikke verge hvis man kun har fremtidsfullmakt`() {
            val vergemaalEllerFremtidsfullmakt = vergemaalEllerFremtidsfullmakt(type = "stadfestetFremtidsfullmakt")
            val pdlSøker = pdlSøker(vergemaalEllerFremtidsfullmakt = listOf(vergemaalEllerFremtidsfullmakt))
            every { personService.hentSøker(any()) } returns pdlSøker

            assertThat(service.hentPersonopplysningerForFagsakPerson(UUID.randomUUID()).harVergemål).isFalse
        }

        @Test
        fun `har verge hvis man har et vergemål`() {
            val pdlSøker = pdlSøker(vergemaalEllerFremtidsfullmakt = listOf(vergemaalEllerFremtidsfullmakt()))
            every { personService.hentSøker(any()) } returns pdlSøker

            assertThat(service.hentPersonopplysningerForFagsakPerson(UUID.randomUUID()).harVergemål).isTrue
        }
    }
}
