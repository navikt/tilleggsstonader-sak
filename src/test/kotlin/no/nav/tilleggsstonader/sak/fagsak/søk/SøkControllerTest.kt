package no.nav.tilleggsstonader.sak.fagsak.søk

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusHarSakerDto
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.felles.PersonIdentDto
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.ArenaClientConfig
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaClient
import no.nav.tilleggsstonader.sak.util.ProblemDetailUtil.catchProblemDetailException
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.exchange

internal class SøkControllerTest : IntegrationTest() {
    @Autowired
    lateinit var arenaClient: ArenaClient

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(onBehalfOfToken())
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        ArenaClientConfig.resetMock(arenaClient)
    }

    @Test
    internal fun `Gitt person med fagsak når søk på personensident kallas skal det returneres 200 OK med Søkeresultat`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("01010199999"))))

        val response = søkPerson("01010199999")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.fagsakPersonId).isEqualTo(fagsak.fagsakPersonId)
        assertThat(response.body?.personIdent).isEqualTo("01010199999")
    }

    @Test
    internal fun `person uten fagsak men finnes i arena skal svare med fagsakPersonId=null`() {
        val response = søkPerson("01010199999")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.fagsakPersonId).isNull()
        assertThat(response.body?.personIdent).isEqualTo("01010199999")
    }

    @Test
    internal fun `person uten fagsak og ikke finnes i arena skal svare med BAD_REQUEST`() {
        every { arenaClient.harSaker(any()) } returns ArenaStatusHarSakerDto(false)
        val response = catchProblemDetailException { søkPerson("01010166666") }

        assertThat(response.responseException.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.detail.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(response.detail.detail).isEqualTo("Personen har ikke fagsak eller sak i arena")
    }

    @Test
    internal fun `Skal feile hvis personIdenten ikke finnes i pdl`() {
        val response = catchProblemDetailException { søkPerson("19117313797") }

        assertThat(response.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.detail.detail).isEqualTo("Finner ingen personer for valgt personident")
    }

    @Test
    internal fun `Skal feile hvis personIdenten har feil lengde`() {
        val response = catchProblemDetailException { søkPerson("010101999990") }

        assertThat(response.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.detail.detail).isEqualTo("Ugyldig personident. Det må være 11 sifre")
    }

    @Test
    internal fun `Skal feile hvis personIdenten inneholder noe annet enn tall`() {
        val response = catchProblemDetailException { søkPerson("010et1ord02") }

        assertThat(response.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.detail.detail).isEqualTo("Ugyldig personident. Det kan kun inneholde tall")
    }

    @Nested
    inner class SøkPersonForEksternFagsak {
        @Test
        internal fun `skal finne person hvis fagsaken eksisterer`() {
            val personIdent = "123"
            val fagsakPerson = testoppsettService.opprettPerson(personIdent)
            val fagsak =
                testoppsettService.lagreFagsak(fagsak(person = fagsakPerson, stønadstype = Stønadstype.BARNETILSYN))

            val response = søkPerson(fagsak.eksternId.id)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val data = response.body!!
            assertThat(data.personIdent).isEqualTo(fagsak.hentAktivIdent())
        }

        @Test
        internal fun `skal kaste feil hvis fagsaken ikke eksisterer`() {
            testoppsettService.lagreFagsak(fagsak())
            val response = catchProblemDetailException { søkPerson(100L) }

            assertThat(response.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            val data = response.detail
            assertThat(data.detail)
                .isEqualTo("Finner ikke fagsak for eksternFagsakId=100")
        }

        private fun søkPerson(eksternFagsakId: Long): ResponseEntity<Søkeresultat> =
            restTemplate.exchange(
                localhost("/api/sok/person/fagsak-ekstern/$eksternFagsakId"),
                HttpMethod.GET,
                HttpEntity<Any>(headers),
            )
    }

    private fun søkPerson(personIdent: String): ResponseEntity<Søkeresultat> =
        restTemplate.exchange(
            localhost("/api/sok"),
            HttpMethod.POST,
            HttpEntity(PersonIdentDto(personIdent = personIdent), headers),
        )
}
