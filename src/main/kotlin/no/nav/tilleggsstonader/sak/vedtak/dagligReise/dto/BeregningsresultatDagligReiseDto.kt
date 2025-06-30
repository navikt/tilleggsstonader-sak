package no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterBeregnUtil.summerUtgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BoutgifterPerUtgiftstype
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

data class BeregningsresultatDagligReiseDto(
    val perioder: List<BeregningsresultatForPeriodeDto>,
    val tidligsteEndring: LocalDate? = null,
)

data class BeregningsresultatForPeriodeDto(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val stønadsbeløp: Int,
    @Deprecated("Skal gå over til å bruke utgifterTilUtbetaling")
    val utgifter: BoutgifterPerUtgiftstype,
    // Kan renames til utgifter når frontend er klar
    val utgifterTilUtbetaling: List<UtgiftDagligReiseMedAndelTilUtbetalingDto>,
    val sumUtgifter: Int,
    val målgruppe: FaktiskMålgruppe,
    val aktivitet: AktivitetType,
    val makssatsBekreftet: Boolean,
    val delAvTidligereUtbetaling: Boolean,
) : Periode<LocalDate>

data class UtgiftDagligReiseMedAndelTilUtbetalingDto(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val utgift: Int,
    val tilUtbetaling: Int,
    val erFørRevurderFra: Boolean,
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }
}

fun BeregningsresultatDagligReise.tilDto(tidligsteEndring: LocalDate?): BeregningsresultatDagligReiseDto =
    BeregningsresultatDagligReiseDto(
        perioder =
            filtrerFraOgMed(tidligsteEndring)
                .perioder
                .map { it.tilDto(tidligsteEndring) },
        tidligsteEndring = tidligsteEndring,
    )

private fun BeregningsresultatDagligReise.filtrerFraOgMed(dato: LocalDate?): BeregningsresultatDagligReise {
    if (dato == null) {
        return this
    }
    return BeregningsresultatDagligReise(perioder.filter { it.tom >= dato })
}

private fun BeregningsresultatBoutgifter.inneholderUtgifterOvernatting(): Boolean =
    perioder.any {
        it.grunnlag.utgifter.keys
            .contains(TypeBoutgift.UTGIFTER_OVERNATTING)
    }

fun BeregningsresultatForLøpendeMåned.tilDto(revurderFra: LocalDate?): BeregningsresultatForPeriodeDto =
    BeregningsresultatForPeriodeDto(
        fom = fom,
        tom = tom,
        stønadsbeløp = stønadsbeløp,
        sumUtgifter = grunnlag.summerUtgifter(),
        utgifter = grunnlag.utgifter,
        utgifterTilUtbetaling = finnUtgifterMedAndelTilUtbetaling(revurderFra),
        målgruppe = grunnlag.målgruppe,
        aktivitet = grunnlag.aktivitet,
        makssatsBekreftet = grunnlag.makssatsBekreftet,
        delAvTidligereUtbetaling = delAvTidligereUtbetaling,
    )

fun BeregningsresultatForLøpendeMåned.finnUtgifterMedAndelTilUtbetaling(
    revurderFra: LocalDate?,
): List<UtgiftDagligReiseMedAndelTilUtbetalingDto> {
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
            UtgiftDagligReiseMedAndelTilUtbetalingDto(
                fom = utgift.fom,
                tom = utgift.tom,
                utgift = utgift.utgift,
                tilUtbetaling = skalUtbetales,
                erFørRevurderFra = revurderFra != null && utgift.tom < revurderFra,
            )
        }
}
