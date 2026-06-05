package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.DagligReiseBeregningService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BeregningsplanUtlederTest {
    @Test
    fun `fraDato skal forskyves 29 dager tilbake for daglig reise`() {
        val opphørsdato = 30 januar 2026

        val beregningsplan =
            BeregningsplanUtleder.utledForOpphørEllerSatsjustering(
                opphørsdato = opphørsdato,
                stønadsspesifikkJusteringAvBeregnFra = DagligReiseBeregningService.justerBeregnFra(),
            )

        assertThat(beregningsplan.omfang).isEqualTo(Beregningsomfang.FRA_DATO)
        assertThat(beregningsplan.fraDato).isEqualTo(1 januar 2026)
    }

    @Test
    fun `fraDato skal være uendret når ingen justeringslambda er satt`() {
        val opphørsdato = 30 januar 2026

        val beregningsplan = BeregningsplanUtleder.utledForOpphørEllerSatsjustering(opphørsdato)

        assertThat(beregningsplan.omfang).isEqualTo(Beregningsomfang.FRA_DATO)
        assertThat(beregningsplan.fraDato).isEqualTo(30 januar 2026)
    }

    @Test
    fun `fraDato for daglig reise kan bli forskjøvet til forrige måned`() {
        val opphørsdato = 15 mars 2026

        val beregningsplan =
            BeregningsplanUtleder.utledForOpphørEllerSatsjustering(
                opphørsdato = opphørsdato,
                stønadsspesifikkJusteringAvBeregnFra = DagligReiseBeregningService.justerBeregnFra(),
            )

        assertThat(beregningsplan.fraDato).isEqualTo(14 februar 2026)
    }
}
