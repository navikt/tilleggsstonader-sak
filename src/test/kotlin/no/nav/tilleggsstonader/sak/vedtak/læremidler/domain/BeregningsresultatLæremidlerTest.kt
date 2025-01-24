package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.beregningsresultatForMåned
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BeregningsresultatLæremidlerTest {
    val beregningsresultat = BeregningsresultatLæremidler(
        perioder = listOf(
            beregningsresultatForMåned(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 31),
                utbetalingsdato = LocalDate.of(2024, 1, 1),
            ),
            beregningsresultatForMåned(
                fom = LocalDate.of(2024, 2, 1),
                tom = LocalDate.of(2024, 2, 29),
                utbetalingsdato = LocalDate.of(2024, 1, 1),
            ),
        ),
    )

    @Test
    fun `filtrerFraOgMed skal filtere vekk perioder før satt dato`() {
        val forventetResultat = BeregningsresultatLæremidler(
            perioder = listOf(
                beregningsresultat.perioder.last(),
            ),
        )
        val result = beregningsresultat.filtrerFraOgMed(LocalDate.of(2024, 2, 21))

        assertThat(result).isEqualTo(forventetResultat)
    }

    @Test
    fun `filtrerFraOgMed skal ikke filtere vekk perioder når inten satt dato`() {
        val result = beregningsresultat.filtrerFraOgMed(LocalDate.of(2024, 1, 1))

        assertThat(result).isEqualTo(beregningsresultat)
    }
}
