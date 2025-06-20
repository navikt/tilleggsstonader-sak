package no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.UtgiftBeregningBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BeregningsresultatBoutgifterDtoTest {
    @Test
    fun `finnUtgifterMedAndelTilUtbetaling skal finne andel til utbetaling når utgifter ikke avkortes mot makssats`() {
        val utgift =
            listOf(
                UtgiftBeregningBoutgifter(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 5),
                    utgift = 1000,
                ),
                UtgiftBeregningBoutgifter(
                    fom = LocalDate.of(2023, 1, 11),
                    tom = LocalDate.of(2023, 1, 16),
                    utgift = 2000,
                ),
            )

        val beregningsresultatForLøpendeMåned =
            BeregningsresultatForLøpendeMåned(
                grunnlag =
                    Beregningsgrunnlag(
                        fom = LocalDate.of(2023, 1, 1),
                        tom = LocalDate.of(2023, 1, 31),
                        utgifter = mapOf(TypeBoutgift.UTGIFTER_OVERNATTING to utgift),
                        makssats = 4953,
                        makssatsBekreftet = true,
                        målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                        aktivitet = AktivitetType.TILTAK,
                    ),
                stønadsbeløp = 3000,
            )

        val forventetResultat =
            listOf(
                UtgiftBoutgifterMedAndelTilUtbetalingDto(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 5),
                    utgift = 1000,
                    tilUtbetaling = 1000,
                    erFørRevurderFra = false,
                ),
                UtgiftBoutgifterMedAndelTilUtbetalingDto(
                    fom = LocalDate.of(2023, 1, 11),
                    tom = LocalDate.of(2023, 1, 16),
                    utgift = 2000,
                    tilUtbetaling = 2000,
                    erFørRevurderFra = false,
                ),
            )

        val result =
            beregningsresultatForLøpendeMåned.finnUtgifterMedAndelTilUtbetaling(null)

        assertEquals(forventetResultat, result)
    }

    @Test
    fun `finnUtgifterMedAndelTilUtbetaling skal finne andel til utbetaling når utgifter avkortes mot makssats`() {
        val utgift =
            listOf(
                UtgiftBeregningBoutgifter(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 5),
                    utgift = 4000,
                ),
                UtgiftBeregningBoutgifter(
                    fom = LocalDate.of(2023, 1, 11),
                    tom = LocalDate.of(2023, 1, 16),
                    utgift = 2000,
                ),
            )

        val beregningsresultatForLøpendeMåned =
            BeregningsresultatForLøpendeMåned(
                grunnlag =
                    Beregningsgrunnlag(
                        fom = LocalDate.of(2023, 1, 1),
                        tom = LocalDate.of(2023, 1, 31),
                        utgifter = mapOf(TypeBoutgift.UTGIFTER_OVERNATTING to utgift),
                        makssats = 4953,
                        makssatsBekreftet = true,
                        målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                        aktivitet = AktivitetType.TILTAK,
                    ),
                stønadsbeløp = 4953,
            )

        val forventetResultat =
            listOf(
                UtgiftBoutgifterMedAndelTilUtbetalingDto(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 5),
                    utgift = 4000,
                    tilUtbetaling = 4000,
                    erFørRevurderFra = false,
                ),
                UtgiftBoutgifterMedAndelTilUtbetalingDto(
                    fom = LocalDate.of(2023, 1, 11),
                    tom = LocalDate.of(2023, 1, 16),
                    utgift = 2000,
                    tilUtbetaling = 953,
                    erFørRevurderFra = false,
                ),
            )

        val result =
            beregningsresultatForLøpendeMåned.finnUtgifterMedAndelTilUtbetaling(null)

        assertEquals(forventetResultat, result)
    }

    @Test
    fun `finnUtgifterMedAndelTilUtbetaling skal markere utgifter med før revurder fra`() {
        val utgift =
            listOf(
                UtgiftBeregningBoutgifter(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 5),
                    utgift = 1000,
                ),
                UtgiftBeregningBoutgifter(
                    fom = LocalDate.of(2023, 1, 11),
                    tom = LocalDate.of(2023, 1, 16),
                    utgift = 2000,
                ),
            )

        val beregningsresultatForLøpendeMåned =
            BeregningsresultatForLøpendeMåned(
                grunnlag =
                    Beregningsgrunnlag(
                        fom = LocalDate.of(2023, 1, 1),
                        tom = LocalDate.of(2023, 1, 31),
                        utgifter = mapOf(TypeBoutgift.UTGIFTER_OVERNATTING to utgift),
                        makssats = 4953,
                        makssatsBekreftet = true,
                        målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                        aktivitet = AktivitetType.TILTAK,
                    ),
                stønadsbeløp = 3000,
            )

        val forventetResultat =
            listOf(
                UtgiftBoutgifterMedAndelTilUtbetalingDto(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 5),
                    utgift = 1000,
                    tilUtbetaling = 1000,
                    erFørRevurderFra = true,
                ),
                UtgiftBoutgifterMedAndelTilUtbetalingDto(
                    fom = LocalDate.of(2023, 1, 11),
                    tom = LocalDate.of(2023, 1, 16),
                    utgift = 2000,
                    tilUtbetaling = 2000,
                    erFørRevurderFra = false,
                ),
            )

        val revurderFra = LocalDate.of(2023, 1, 10)

        val result =
            beregningsresultatForLøpendeMåned.finnUtgifterMedAndelTilUtbetaling(revurderFra)

        assertEquals(forventetResultat, result)
    }

    @Test
    fun `skal vise riktig beløp hvis man har fått høyere utgifter`() {
        val utgift =
            listOf(
                UtgiftBeregningBoutgifter(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 5),
                    utgift = 20_000,
                    skalFåDekketFaktiskeUtgifter = true,
                ),
            )

        val beregningsresultatForLøpendeMåned =
            BeregningsresultatForLøpendeMåned(
                grunnlag =
                    Beregningsgrunnlag(
                        fom = LocalDate.of(2023, 1, 1),
                        tom = LocalDate.of(2023, 1, 31),
                        utgifter = mapOf(TypeBoutgift.UTGIFTER_OVERNATTING to utgift),
                        makssats = 4953,
                        makssatsBekreftet = true,
                        målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                        aktivitet = AktivitetType.TILTAK,
                    ),
                stønadsbeløp = 20_000,
            )

        val forventetResultat =
            listOf(
                UtgiftBoutgifterMedAndelTilUtbetalingDto(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 5),
                    utgift = 20_000,
                    tilUtbetaling = 20_000,
                    erFørRevurderFra = true,
                ),
            )

        val revurderFra = LocalDate.of(2023, 1, 10)

        val result =
            beregningsresultatForLøpendeMåned.finnUtgifterMedAndelTilUtbetaling(revurderFra)

        assertEquals(forventetResultat, result)
    }
}
