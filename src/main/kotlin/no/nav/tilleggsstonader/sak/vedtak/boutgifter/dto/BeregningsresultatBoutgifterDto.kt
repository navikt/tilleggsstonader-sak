package no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterBeregnUtil.summerUtgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

data class BeregningsresultatBoutgifterDto(
    val perioder: List<BeregningsresultatForPeriodeDto>,
    val inneholderUtgifterOvernatting: Boolean,
    val tidligsteEndring: LocalDate? = null,
)

data class BeregningsresultatForPeriodeDto(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val stønadsbeløp: Int,
    val utgifter: List<UtgiftBoutgifterMedAndelTilUtbetalingDto>,
    val sumUtgifter: Int,
    val målgruppe: FaktiskMålgruppe,
    val aktivitet: AktivitetType,
    val makssatsBekreftet: Boolean,
    val delAvTidligereUtbetaling: Boolean,
    val skalFåDekketFaktiskeUtgifter: Boolean,
    val inneholderUtgifterOvernatting: Boolean,
) : Periode<LocalDate>

data class UtgiftBoutgifterMedAndelTilUtbetalingDto(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val utgift: Int,
    val tilUtbetaling: Int,
    val erFørTidligsteEndring: Boolean,
    val skalFåDekketFaktiskeUtgifter: Boolean,
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }
}

fun BeregningsresultatBoutgifter.tilDto(tidligsteEndring: LocalDate?): BeregningsresultatBoutgifterDto =
    BeregningsresultatBoutgifterDto(
        perioder =
            filtrerFraOgMed(tidligsteEndring)
                .perioder
                .map { it.tilDto(tidligsteEndring) },
        inneholderUtgifterOvernatting = inneholderUtgifterOvernatting(),
        tidligsteEndring = tidligsteEndring,
    )

private fun BeregningsresultatBoutgifter.filtrerFraOgMed(dato: LocalDate?): BeregningsresultatBoutgifter {
    if (dato == null) {
        return this
    }
    return BeregningsresultatBoutgifter(perioder.filter { it.tom >= dato })
}

private fun BeregningsresultatBoutgifter.inneholderUtgifterOvernatting(): Boolean =
    perioder.any {
        it.grunnlag.utgifter.keys
            .contains(TypeBoutgift.UTGIFTER_OVERNATTING)
    }

fun BeregningsresultatForLøpendeMåned.tilDto(tidligsteEndring: LocalDate?): BeregningsresultatForPeriodeDto =
    BeregningsresultatForPeriodeDto(
        fom = fom,
        tom = tom,
        stønadsbeløp = stønadsbeløp,
        sumUtgifter = grunnlag.summerUtgifter(),
        utgifter = finnUtgifterMedAndelTilUtbetaling(tidligsteEndring),
        målgruppe = grunnlag.målgruppe,
        aktivitet = grunnlag.aktivitet,
        makssatsBekreftet = grunnlag.makssatsBekreftet,
        delAvTidligereUtbetaling = delAvTidligereUtbetaling,
        skalFåDekketFaktiskeUtgifter = grunnlag.skalFåDekketFaktiskeUtgifter(),
        inneholderUtgifterOvernatting = !grunnlag.utgifter[TypeBoutgift.UTGIFTER_OVERNATTING].isNullOrEmpty(),
    )

fun BeregningsresultatForLøpendeMåned.finnUtgifterMedAndelTilUtbetaling(
    tidligsteEndring: LocalDate?,
): List<UtgiftBoutgifterMedAndelTilUtbetalingDto> {
    var totalSum = 0
    return grunnlag.utgifter.values
        .flatten()
        .sorted()
        .map { utgift ->
            val skalUtbetales =
                if (grunnlag.skalFåDekketFaktiskeUtgifter()) {
                    utgift.utgift
                } else {
                    minOf(utgift.utgift, grunnlag.makssats - totalSum)
                }
            totalSum += skalUtbetales
            val erFørTidligsteEndring = tidligsteEndring != null && utgift.tom < tidligsteEndring
            UtgiftBoutgifterMedAndelTilUtbetalingDto(
                fom = utgift.fom,
                tom = utgift.tom,
                utgift = utgift.utgift,
                tilUtbetaling = skalUtbetales,
                erFørTidligsteEndring = erFørTidligsteEndring,
                skalFåDekketFaktiskeUtgifter = utgift.skalFåDekketFaktiskeUtgifter,
            )
        }
}
