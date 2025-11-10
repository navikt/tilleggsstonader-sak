package no.nav.tilleggsstonader.sak.fagsak.søk

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusHarSakerDto
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectBadRequestWithDetail
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaClient
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class SøkControllerTest : IntegrationTest() {
    @Autowired
    lateinit var arenaClient: ArenaClient

    @Test
    internal fun `Gitt person med fagsak når søk på personensident kallas skal det returneres 200 OK med Søkeresultat`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("01010199999"))))

        val response = kall.person.sok("01010199999")

        assertThat(response.fagsakPersonId).isEqualTo(fagsak.fagsakPersonId)
        assertThat(response.personIdent).isEqualTo("01010199999")
    }

    @Test
    internal fun `person uten fagsak men finnes i arena skal svare med fagsakPersonId=null`() {
        val response = kall.person.sok("01010199999")

        assertThat(response.fagsakPersonId).isNull()
        assertThat(response.personIdent).isEqualTo("01010199999")
    }

    @Test
    internal fun `person uten fagsak og ikke finnes i arena skal svare med BAD_REQUEST`() {
        every { arenaClient.harSaker(any()) } returns ArenaStatusHarSakerDto(false)

        kall.person
            .sokResponse("01010166666")
            .expectBadRequestWithDetail("Personen har ikke fagsak eller sak i arena")
    }

    @Test
    internal fun `Skal feile hvis personIdenten ikke finnes i pdl`() {
        kall.person
            .sokResponse("19117313797")
            .expectBadRequestWithDetail("Finner ingen personer for valgt personident")
    }

    @Test
    internal fun `Skal feile hvis personIdenten har feil lengde`() {
        kall.person
            .sokResponse("010101999990")
            .expectBadRequestWithDetail("Ugyldig personident. Det må være 11 sifre")
    }

    @Test
    internal fun `Skal feile hvis personIdenten inneholder noe annet enn tall`() {
        kall.person
            .sokResponse("010et1ord02")
            .expectBadRequestWithDetail("Ugyldig personident. Det kan kun inneholde tall")
    }

    @Nested
    inner class SøkPersonForEksternFagsak {
        @Test
        internal fun `skal finne person hvis fagsaken eksisterer`() {
            val personIdent = "123"
            val fagsakPerson = testoppsettService.opprettPerson(personIdent)
            val fagsak =
                testoppsettService.lagreFagsak(fagsak(person = fagsakPerson, stønadstype = Stønadstype.BARNETILSYN))

            val data = kall.person.fagsakEkstern(fagsak.eksternId.id)
            assertThat(data.personIdent).isEqualTo(fagsak.hentAktivIdent())
        }

        @Test
        internal fun `skal kaste feil hvis fagsaken ikke eksisterer`() {
            kall.person
                .fagsakEksternResponse(100L)
                .expectBadRequestWithDetail("Finner ikke fagsak for eksternFagsakId=100")
        }
    }
}
