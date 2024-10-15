package no.nav.tilleggsstonader.sak.vedtak.læremidler

import java.math.BigDecimal
import java.time.YearMonth
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LæremidlerBeregningServiceTest() {
    val læremidlerBeregningService = LæremidlerBeregningService()

    @Test
    fun `høyere utdanning full sats for 2024`() {
        val læremidler = læremidlerBeregningService.beregn(Studienivå.HØYERE_UTDANNING, 100, YearMonth.of(2024, 1))
        assertEquals(BigDecimal(875), læremidler)
    }

    @Test
    fun `videregående utdanning full sats for 2024`() {
        val læremidler = læremidlerBeregningService.beregn(Studienivå.VIDEREGÅENDE, 100, YearMonth.of(2024, 1))
        assertEquals(BigDecimal(438), læremidler)
    }

    @Test
    fun `høyere utdanning halv sats for 2024`() {
        val læremidler = læremidlerBeregningService.beregn(Studienivå.HØYERE_UTDANNING, 50, YearMonth.of(2024, 1))
        assertEquals(BigDecimal(438), læremidler)
    }

    @Test
    fun `videregående utdanning halv sats for 2024`() {
        val læremidler = læremidlerBeregningService.beregn(Studienivå.VIDEREGÅENDE, 50, YearMonth.of(2024, 1))
        assertEquals(BigDecimal(219), læremidler)
    }

    @Test
    fun `høyere utdanning full sats for 2023`() {
        val læremidler = læremidlerBeregningService.beregn(Studienivå.HØYERE_UTDANNING, 100, YearMonth.of(2023, 1))
        assertEquals(BigDecimal(822), læremidler)
    }

    @Test
    fun `videregående utdanning full sats for 2023`() {
        val læremidler = læremidlerBeregningService.beregn(Studienivå.VIDEREGÅENDE, 100, YearMonth.of(2023, 1))
        assertEquals(BigDecimal(411), læremidler)
    }

    @Test
    fun `høyere utdanning halv sats for 2023`() {
        val læremidler = læremidlerBeregningService.beregn(Studienivå.HØYERE_UTDANNING, 50, YearMonth.of(2023, 1))
        assertEquals(BigDecimal(411), læremidler)
    }

    @Test
    fun `videregående utdanning halv sats for 2023`() {
        val læremidler = læremidlerBeregningService.beregn(Studienivå.VIDEREGÅENDE, 50, YearMonth.of(2023, 1))
        assertEquals(BigDecimal(206), læremidler)
    }
}