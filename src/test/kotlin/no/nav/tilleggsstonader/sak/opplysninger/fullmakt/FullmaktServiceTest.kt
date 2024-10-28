package no.nav.tilleggsstonader.sak.opplysninger.fullmakt

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.fullmakt.FullmektigDto
import no.nav.tilleggsstonader.sak.util.FullmektigStubs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FullmaktServiceTest {
    private val mockFullmaktClient = mockk<FullmaktClient>()
    private val serviceUnderTest = FullmaktService(mockFullmaktClient)

    @Test
    fun `hentFullmektige skal filtere bort inaktive fullmakter`() {
        mockFullmektigeRespons(listOf(FullmektigStubs.ikkeGyldigEnda, FullmektigStubs.utgått))
        val resultat = serviceUnderTest.hentFullmektige(dummyIdent)
        assertEquals(emptyList<FullmektigDto>(), resultat)
    }

    @Test
    fun `hentFullmektige tar bare med fullmakter med Tilleggsstønad-tema`() {
        mockFullmektigeRespons(listOf(FullmektigStubs.ikkeRelevantTema, FullmektigStubs.medFlereTemaer))
        val resultat = serviceUnderTest.hentFullmektige(dummyIdent)
        assertThat(resultat.size).isEqualTo(1)
        assertEquals(resultat.first(), FullmektigStubs.medFlereTemaer)
    }

    @Test
    fun `hentFullmektige inkluderer fullmakter med ubegrenset gyldighetsperiode`() {
        mockFullmektigeRespons(listOf(FullmektigStubs.gyldigPåUbestemtTid))
        val resultat = serviceUnderTest.hentFullmektige(dummyIdent)
        assertThat(resultat.size).isEqualTo(1)
        assertEquals(resultat.first(), FullmektigStubs.gyldigPåUbestemtTid)
    }

    private fun mockFullmektigeRespons(fullmakter: List<FullmektigDto>) {
        every { mockFullmaktClient.hentFullmektige(any()) } returns (fullmakter)
    }
}

private val dummyIdent = FullmektigStubs.gyldig.fullmektigIdent
