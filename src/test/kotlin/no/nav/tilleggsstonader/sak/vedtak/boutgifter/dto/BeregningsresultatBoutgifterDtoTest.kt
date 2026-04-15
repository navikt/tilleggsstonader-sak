package no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto

import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.lagUtgiftBeregningBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BeregningsresultatBoutgifterDtoTest {
    @Test
    fun `skal ikke filtrere perioder ved gjenbruk av forrige resultat`() {
        val dto =
            BeregningsresultatBoutgifter(
                perioder =
                    listOf(
                        beregningsresultatForLøpendeMåned(1 januar 2023, 31 januar 2023, 3000),
                        beregningsresultatForLøpendeMåned(1 februar 2023, 28 februar 2023, 4000),
                    ),
            ).tilDto(Beregningsplan(Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT))

        assertEquals(listOf(1 januar 2023, 1 februar 2023), dto.perioder.map { it.fom })
        assertEquals(null, dto.tidligsteEndring)
    }

    @Test
    fun `finnUtgifterMedAndelTilUtbetaling skal finne andel til utbetaling når utgifter ikke avkortes mot makssats`() {
        val utgift =
            listOf(
                lagUtgiftBeregningBoutgifter(
                    fom = 1 januar 2023,
                    tom = 5 januar 2023,
                    utgift = 1000,
                ),
                lagUtgiftBeregningBoutgifter(
                    fom = 11 januar 2023,
                    tom = 16 januar 2023,
                    utgift = 2000,
                ),
            )

        val beregningsresultatForLøpendeMåned =
            BeregningsresultatForLøpendeMåned(
                grunnlag =
                    Beregningsgrunnlag(
                        fom = 1 januar 2023,
                        tom = 31 januar 2023,
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
                    fom = 1 januar 2023,
                    tom = 5 januar 2023,
                    utgift = 1000,
                    tilUtbetaling = 1000,
                    erFørTidligsteEndring = false,
                    skalFåDekketFaktiskeUtgifter = false,
                ),
                UtgiftBoutgifterMedAndelTilUtbetalingDto(
                    fom = 11 januar 2023,
                    tom = 16 januar 2023,
                    utgift = 2000,
                    tilUtbetaling = 2000,
                    erFørTidligsteEndring = false,
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
                    fom = 1 januar 2023,
                    tom = 5 januar 2023,
                    utgift = 4000,
                ),
                lagUtgiftBeregningBoutgifter(
                    fom = 11 januar 2023,
                    tom = 16 januar 2023,
                    utgift = 2000,
                ),
            )

        val beregningsresultatForLøpendeMåned =
            BeregningsresultatForLøpendeMåned(
                grunnlag =
                    Beregningsgrunnlag(
                        fom = 1 januar 2023,
                        tom = 31 januar 2023,
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
                    fom = 1 januar 2023,
                    tom = 5 januar 2023,
                    utgift = 4000,
                    tilUtbetaling = 4000,
                    erFørTidligsteEndring = false,
                    skalFåDekketFaktiskeUtgifter = false,
                ),
                UtgiftBoutgifterMedAndelTilUtbetalingDto(
                    fom = 11 januar 2023,
                    tom = 16 januar 2023,
                    utgift = 2000,
                    tilUtbetaling = 953,
                    erFørTidligsteEndring = false,
                    skalFåDekketFaktiskeUtgifter = false,
                ),
            )

        val result =
            beregningsresultatForLøpendeMåned.finnUtgifterMedAndelTilUtbetaling(null)

        assertEquals(forventetResultat, result)
    }

    @Test
    fun `finnUtgifterMedAndelTilUtbetaling skal markere utgifter med før tidligsteEndring`() {
        val utgift =
            listOf(
                lagUtgiftBeregningBoutgifter(
                    fom = 1 januar 2023,
                    tom = 5 januar 2023,
                    utgift = 1000,
                ),
                lagUtgiftBeregningBoutgifter(
                    fom = 11 januar 2023,
                    tom = 16 januar 2023,
                    utgift = 2000,
                ),
            )

        val beregningsresultatForLøpendeMåned =
            BeregningsresultatForLøpendeMåned(
                grunnlag =
                    Beregningsgrunnlag(
                        fom = 1 januar 2023,
                        tom = 31 januar 2023,
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
                    fom = 1 januar 2023,
                    tom = 5 januar 2023,
                    utgift = 1000,
                    tilUtbetaling = 1000,
                    erFørTidligsteEndring = true,
                    skalFåDekketFaktiskeUtgifter = false,
                ),
                UtgiftBoutgifterMedAndelTilUtbetalingDto(
                    fom = 11 januar 2023,
                    tom = 16 januar 2023,
                    utgift = 2000,
                    tilUtbetaling = 2000,
                    erFørTidligsteEndring = false,
                    skalFåDekketFaktiskeUtgifter = false,
                ),
            )

        val tidligsteEndring = 10 januar 2023

        val result =
            beregningsresultatForLøpendeMåned.finnUtgifterMedAndelTilUtbetaling(tidligsteEndring)

        assertEquals(forventetResultat, result)
    }

    @Test
    fun `skal vise riktig beløp hvis man har fått høyere utgifter`() {
        val utgift =
            listOf(
                lagUtgiftBeregningBoutgifter(
                    fom = 1 januar 2023,
                    tom = 5 januar 2023,
                    utgift = 20_000,
                    skalFåDekketFaktiskeUtgifter = true,
                ),
            )

        val beregningsresultatForLøpendeMåned =
            BeregningsresultatForLøpendeMåned(
                grunnlag =
                    Beregningsgrunnlag(
                        fom = 1 januar 2023,
                        tom = 31 januar 2023,
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
                    fom = 1 januar 2023,
                    tom = 5 januar 2023,
                    utgift = 20_000,
                    tilUtbetaling = 20_000,
                    erFørTidligsteEndring = true,
                    skalFåDekketFaktiskeUtgifter = true,
                ),
            )

        val tidligsteEndring = 10 januar 2023

        val result =
            beregningsresultatForLøpendeMåned.finnUtgifterMedAndelTilUtbetaling(tidligsteEndring)

        assertEquals(forventetResultat, result)
    }

    @Nested
    inner class BeregningsresultatForLøpendeMånedTilDto {
        @Test
        fun `skal vise om perioden inneholder utgift til overnatting`() {
            val utgiftOvernatting =
                listOf(
                    lagUtgiftBeregningBoutgifter(
                        fom = 1 januar 2023,
                        tom = 5 januar 2023,
                        utgift = 3000,
                        skalFåDekketFaktiskeUtgifter = false,
                    ),
                )

            val beregningsresultatForLøpendeMåned =
                BeregningsresultatForLøpendeMåned(
                    grunnlag =
                        Beregningsgrunnlag(
                            fom = 1 januar 2023,
                            tom = 31 januar 2023,
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
                    fom = 1 januar 2023,
                    tom = 31 januar 2023,
                    stønadsbeløp = 3000,
                    utgifter =
                        listOf(
                            UtgiftBoutgifterMedAndelTilUtbetalingDto(
                                fom = 1 januar 2023,
                                tom = 5 januar 2023,
                                utgift = 3000,
                                tilUtbetaling = 3000,
                                erFørTidligsteEndring = true,
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

            val tidligsteEndring = 10 januar 2023

            val result =
                beregningsresultatForLøpendeMåned.tilDto(tidligsteEndring)

            assertEquals(forventetResultat, result)
        }

        @Test
        fun `skal vise om perioden ikke inneholder utgift til overnatting`() {
            val utgift =
                listOf(
                    lagUtgiftBeregningBoutgifter(
                        fom = 1 januar 2023,
                        tom = 31 januar 2023,
                        utgift = 3000,
                        skalFåDekketFaktiskeUtgifter = false,
                    ),
                )

            val beregningsresultatForLøpendeMåned =
                BeregningsresultatForLøpendeMåned(
                    grunnlag =
                        Beregningsgrunnlag(
                            fom = 1 januar 2023,
                            tom = 31 januar 2023,
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
                    fom = 1 januar 2023,
                    tom = 31 januar 2023,
                    stønadsbeløp = 3000,
                    utgifter =
                        listOf(
                            UtgiftBoutgifterMedAndelTilUtbetalingDto(
                                fom = 1 januar 2023,
                                tom = 31 januar 2023,
                                utgift = 3000,
                                tilUtbetaling = 3000,
                                erFørTidligsteEndring = false,
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

            val tidligsteEndring = 10 januar 2023

            val result =
                beregningsresultatForLøpendeMåned.tilDto(tidligsteEndring)

            assertEquals(forventetResultat, result)
        }
    }

    private fun beregningsresultatForLøpendeMåned(
        fom: LocalDate,
        tom: LocalDate,
        stønadsbeløp: Int,
    ) = BeregningsresultatForLøpendeMåned(
        grunnlag =
            Beregningsgrunnlag(
                fom = fom,
                tom = tom,
                utgifter = mapOf(TypeBoutgift.UTGIFTER_OVERNATTING to emptyList()),
                makssats = 4953,
                makssatsBekreftet = true,
                målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                aktivitet = AktivitetType.TILTAK,
            ),
        stønadsbeløp = stønadsbeløp,
    )
}
