package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.libs.utils.dato.januar
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BeregningsplanUtlederTest {
    @Test
    fun `fraDato skal være uendret når ingen justeringslambda er satt`() {
        val opphørsdato = 30 januar 2026

        val beregningsplan = BeregningsplanUtleder.utledForOpphørEllerSatsjustering(opphørsdato)

        assertThat(beregningsplan.omfang).isEqualTo(Beregningsomfang.FRA_DATO)
        assertThat(beregningsplan.fraDato).isEqualTo(30 januar 2026)
    }

    @Test
    fun `fraDato endrer seg med justeringslambda`() {
        val opphørsdato = 20 januar 2026
        val beregningsplan = BeregningsplanUtleder.utledForOpphørEllerSatsjustering(opphørsdato) { it.plusDays(1) }

        assertThat(beregningsplan.omfang).isEqualTo(Beregningsomfang.FRA_DATO)
        assertThat(beregningsplan.fraDato).isEqualTo(21 januar 2026)
    }
}
