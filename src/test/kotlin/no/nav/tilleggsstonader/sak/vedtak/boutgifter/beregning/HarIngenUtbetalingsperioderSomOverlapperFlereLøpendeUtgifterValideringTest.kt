package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BoutgifterPerUtgiftstype
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.YearMonth

class HarIngenUtbetalingsperioderSomOverlapperFlereLøpendeUtgifterValideringTest {

    val jan = YearMonth.of(2025, 1)
    val feb = YearMonth.of(2025, 2)
    val mars = YearMonth.of(2025, 3)
    val april = YearMonth.of(2025, 3)

    val utbetalingsperioder = listOf(
        utbetalingsperiode(jan),
        utbetalingsperiode(feb),
        utbetalingsperiode(mars),
        utbetalingsperiode(april),
    )

    val utgifter: BoutgifterPerUtgiftstype = mapOf(
        TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to listOf(
            utgift(jan), utgift(feb)
        ),
        TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER to listOf(
            utgift(jan), utgift(feb)
        )
    )

    @Test
    fun `verifiserer at feilmeldingen blir som forventet`() {
        assertThatThrownBy {
            utbetalingsperioder.validerIngenUtbetalingsperioderOverlapperFlereLøpendeUtgifter(utgifter)
        }.hasMessage(
            """
            Vi støtter foreløpig ikke at utbetalingsperioder overlapper mer enn én løpende utgift. 
            Utbetalingsperioder for denne behandlingen er: [01.01.2025–31.01.2025, 01.02.2025–28.02.2025, 01.03.2025–31.03.2025, 01.03.2025–31.03.2025], 
            mens utgiftsperiodene er: [01.01.2025–31.01.2025, 01.02.2025–28.02.2025, 01.01.2025–31.01.2025, 01.02.2025–28.02.2025]
            """.trimIndent()
        )
    }

    private fun utbetalingsperiode(month: YearMonth): UtbetalingPeriode = UtbetalingPeriode(
        LøpendeMåned(
            fom = month.atDay(1),
            tom = month.atEndOfMonth(),
        ).medVedtaksperiode(
            VedtaksperiodeInnenforLøpendeMåned(
                fom = month.atDay(1),
                tom = month.atEndOfMonth(),
                målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                aktivitet = AktivitetType.TILTAK,
            )
        ),
        skalAvkorte = false,
    )

    private fun utgift(month: YearMonth): UtgiftBeregningBoutgifter = UtgiftBeregningBoutgifter(
        fom = month.atDay(1),
        tom = month.atEndOfMonth(),
        utgift = 1000,
        skalFåDekketFaktiskeUtgifter = true,
    )
}