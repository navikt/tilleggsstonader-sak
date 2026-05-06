package no.nav.tilleggsstonader.sak.brev.kjørelistebrev

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class KjørelisteBehandlingBrevServiceTest {
    private val service =
        KjørelisteBehandlingBrevService(
            kjørelisteBehandlingBrevRepository = mockk(),
            familieDokumentClient = mockk(),
            htmlifyClient = mockk(),
            personService = mockk(),
            vedtakService = mockk(),
            behandlingService = mockk(),
        )

    @Nested
    inner class UtledBegrunnelse {
        @Test
        fun `null i request bevarer eksisterende begrunnelse`() {
            val resultat = service.bevarEllerOppdaterBegrunnelse(nyBegrunnelse = null, eksisterendeBegrunnelse = "eksisterende tekst")
            assertThat(resultat).isEqualTo("eksisterende tekst")
        }

        @Test
        fun `null i request returnerer null når det ikke finnes eksisterende begrunnelse`() {
            val resultat = service.bevarEllerOppdaterBegrunnelse(nyBegrunnelse = null, eksisterendeBegrunnelse = null)
            assertThat(resultat).isNull()
        }

        @Test
        fun `tom streng fjerner begrunnelsen`() {
            val resultat = service.bevarEllerOppdaterBegrunnelse(nyBegrunnelse = "", eksisterendeBegrunnelse = "eksisterende tekst")
            assertThat(resultat).isNull()
        }

        @Test
        fun `streng med kun whitespace fjerner begrunnelsen`() {
            val resultat = service.bevarEllerOppdaterBegrunnelse(nyBegrunnelse = "   ", eksisterendeBegrunnelse = "eksisterende tekst")
            assertThat(resultat).isNull()
        }

        @Test
        fun `eksplisitt tekst overskriver eksisterende begrunnelse`() {
            val resultat = service.bevarEllerOppdaterBegrunnelse(nyBegrunnelse = "ny tekst", eksisterendeBegrunnelse = "gammel tekst")
            assertThat(resultat).isEqualTo("ny tekst")
        }

        @Test
        fun `eksplisitt tekst lagres selv om det ikke finnes noen eksisterende`() {
            val resultat = service.bevarEllerOppdaterBegrunnelse(nyBegrunnelse = "ny tekst", eksisterendeBegrunnelse = null)
            assertThat(resultat).isEqualTo("ny tekst")
        }
    }
}
