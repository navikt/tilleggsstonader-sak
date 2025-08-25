package no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.lagUtgiftBeregningBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BeregningsresultatBoutgifterDtoTest {
    @Test
    fun `finnUtgifterMedAndelTilUtbetaling skal finne andel til utbetaling når utgifter ikke avkortes mot makssats`() {
        val utgift =
            listOf(
                lagUtgiftBeregningBoutgifter(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 5),
                    utgift = 1000,
                ),
                lagUtgiftBeregningBoutgifter(
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
                    skalFåDekketFaktiskeUtgifter = false,
                ),
                UtgiftBoutgifterMedAndelTilUtbetalingDto(
                    fom = LocalDate.of(2023, 1, 11),
                    tom = LocalDate.of(2023, 1, 16),
                    utgift = 2000,
                    tilUtbetaling = 2000,
                    erFørRevurderFra = false,
                    skalFåDekketFaktiskeUtgifter = false,
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
                lagUtgiftBeregningBoutgifter(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 5),
                    utgift = 4000,
                ),
                lagUtgiftBeregningBoutgifter(
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
                    skalFåDekketFaktiskeUtgifter = false,
                ),
                UtgiftBoutgifterMedAndelTilUtbetalingDto(
                    fom = LocalDate.of(2023, 1, 11),
                    tom = LocalDate.of(2023, 1, 16),
                    utgift = 2000,
                    tilUtbetaling = 953,
                    erFørRevurderFra = false,
                    skalFåDekketFaktiskeUtgifter = false,
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
                lagUtgiftBeregningBoutgifter(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 5),
                    utgift = 1000,
                ),
                lagUtgiftBeregningBoutgifter(
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
                    skalFåDekketFaktiskeUtgifter = false,
                ),
                UtgiftBoutgifterMedAndelTilUtbetalingDto(
                    fom = LocalDate.of(2023, 1, 11),
                    tom = LocalDate.of(2023, 1, 16),
                    utgift = 2000,
                    tilUtbetaling = 2000,
                    erFørRevurderFra = false,
                    skalFåDekketFaktiskeUtgifter = false,
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
                lagUtgiftBeregningBoutgifter(
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
                    skalFåDekketFaktiskeUtgifter = true,
                ),
            )

        val revurderFra = LocalDate.of(2023, 1, 10)

        val result =
            beregningsresultatForLøpendeMåned.finnUtgifterMedAndelTilUtbetaling(revurderFra)

        assertEquals(forventetResultat, result)
    }

    @Nested
    inner class BeregningsresultatForLøpendeMånedTilDto {
        @Test
        fun `skal vise om perioden inneholder utgift til overnatting`() {
            val utgiftOvernatting =
                listOf(
                    lagUtgiftBeregningBoutgifter(
                        fom = LocalDate.of(2023, 1, 1),
                        tom = LocalDate.of(2023, 1, 5),
                        utgift = 3000,
                        skalFåDekketFaktiskeUtgifter = false,
                    ),
                )

            val beregningsresultatForLøpendeMåned =
                BeregningsresultatForLøpendeMåned(
                    grunnlag =
                        Beregningsgrunnlag(
                            fom = LocalDate.of(2023, 1, 1),
                            tom = LocalDate.of(2023, 1, 31),
                            utgifter = mapOf(TypeBoutgift.UTGIFTER_OVERNATTING to utgiftOvernatting),
                            makssats = 4953,
                            makssatsBekreftet = true,
                            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                            aktivitet = AktivitetType.TILTAK,
                        ),
                    stønadsbeløp = 3000,
                )

            val forventetResultat =
                BeregningsresultatForPeriodeDto(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 31),
                    stønadsbeløp = 3000,
                    utgifter =
                        listOf(
                            UtgiftBoutgifterMedAndelTilUtbetalingDto(
                                fom = LocalDate.of(2023, 1, 1),
                                tom = LocalDate.of(2023, 1, 5),
                                utgift = 3000,
                                tilUtbetaling = 3000,
                                erFørRevurderFra = true,
                                skalFåDekketFaktiskeUtgifter = false,
                            ),
                        ),
                    sumUtgifter = 3000,
                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                    aktivitet = AktivitetType.TILTAK,
                    makssatsBekreftet = true,
                    delAvTidligereUtbetaling = false,
                    skalFåDekketFaktiskeUtgifter = false,
                    inneholderUtgifterOvernatting = true,
                )

            val revurderFra = LocalDate.of(2023, 1, 10)

            val result =
                beregningsresultatForLøpendeMåned.tilDto(revurderFra)

            assertEquals(forventetResultat, result)
        }

        @Test
        fun `skal vise om perioden ikke inneholder utgift til overnatting`() {
            val utgift =
                listOf(
                    lagUtgiftBeregningBoutgifter(
                        fom = LocalDate.of(2023, 1, 1),
                        tom = LocalDate.of(2023, 1, 31),
                        utgift = 3000,
                        skalFåDekketFaktiskeUtgifter = false,
                    ),
                )

            val beregningsresultatForLøpendeMåned =
                BeregningsresultatForLøpendeMåned(
                    grunnlag =
                        Beregningsgrunnlag(
                            fom = LocalDate.of(2023, 1, 1),
                            tom = LocalDate.of(2023, 1, 31),
                            utgifter =
                                mapOf(
                                    TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to utgift,
                                    TypeBoutgift.UTGIFTER_OVERNATTING to emptyList(),
                                ),
                            makssats = 4953,
                            makssatsBekreftet = true,
                            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                            aktivitet = AktivitetType.TILTAK,
                        ),
                    stønadsbeløp = 3000,
                )

            val forventetResultat =
                BeregningsresultatForPeriodeDto(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 31),
                    stønadsbeløp = 3000,
                    utgifter =
                        listOf(
                            UtgiftBoutgifterMedAndelTilUtbetalingDto(
                                fom = LocalDate.of(2023, 1, 1),
                                tom = LocalDate.of(2023, 1, 31),
                                utgift = 3000,
                                tilUtbetaling = 3000,
                                erFørRevurderFra = false,
                                skalFåDekketFaktiskeUtgifter = false,
                            ),
                        ),
                    sumUtgifter = 3000,
                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                    aktivitet = AktivitetType.TILTAK,
                    makssatsBekreftet = true,
                    delAvTidligereUtbetaling = false,
                    skalFåDekketFaktiskeUtgifter = false,
                    inneholderUtgifterOvernatting = false,
                )

            val revurderFra = LocalDate.of(2023, 1, 10)

            val result =
                beregningsresultatForLøpendeMåned.tilDto(revurderFra)

            assertEquals(forventetResultat, result)
        }
    }
}
