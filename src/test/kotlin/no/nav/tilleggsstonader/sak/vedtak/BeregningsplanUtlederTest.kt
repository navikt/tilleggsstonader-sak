package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BeregningsplanUtlederTest {
    @ParameterizedTest
    @EnumSource(value = Stønadstype::class, names = ["DAGLIG_REISE_TSO", "DAGLIG_REISE_TSR"])
    fun `fraDato skal forskyves 29 dager tilbake for daglig reise`(stønadstype: Stønadstype) {
        val opphørsdato = 30 januar 2026

        val beregningsplan = BeregningsplanUtleder.utledForOpphørEllerSatsjustering(stønadstype, opphørsdato)

        assertThat(beregningsplan.omfang).isEqualTo(Beregningsomfang.FRA_DATO)
        assertThat(beregningsplan.fraDato).isEqualTo(1 januar 2026)
    }

    @ParameterizedTest
    @EnumSource(value = Stønadstype::class, names = ["DAGLIG_REISE_TSO", "DAGLIG_REISE_TSR"], mode = EnumSource.Mode.EXCLUDE)
    fun `fraDato skal være uendret for øvrige stønadstyper`(stønadstype: Stønadstype) {
        val opphørsdato = 30 januar 2026

        val beregningsplan = BeregningsplanUtleder.utledForOpphørEllerSatsjustering(stønadstype, opphørsdato)

        assertThat(beregningsplan.omfang).isEqualTo(Beregningsomfang.FRA_DATO)
        assertThat(beregningsplan.fraDato).isEqualTo(30 januar 2026)
    }

    @Test
    fun `fraDato for daglig reise kan bli forskjøvet til forrige måned`() {
        val opphørsdato = 15 mars 2026

        val beregningsplan =
            BeregningsplanUtleder.utledForOpphørEllerSatsjustering(Stønadstype.DAGLIG_REISE_TSO, opphørsdato)

        assertThat(beregningsplan.fraDato).isEqualTo(14 februar 2026)
    }
}
