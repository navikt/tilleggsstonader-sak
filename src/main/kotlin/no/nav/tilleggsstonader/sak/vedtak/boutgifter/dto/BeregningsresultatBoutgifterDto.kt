package no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.UtgiftBeregningBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

data class BeregningsresultatBoutgifterDto(
    val perioder: List<BeregningsresultatForPeriodeDto>,
    val skalBrukeDetaljertVisning: Boolean,
)

data class BeregningsresultatForPeriodeDto(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val stønadsbeløp: Int,
    val utbetalingsdato: LocalDate,
    val utgifter: List<UtgiftBeregningBoutgifter>,
    val sumUtgifter: Int,
    val målgruppe: FaktiskMålgruppe,
    val aktivitet: AktivitetType,
    val makssatsBekreftet: Boolean,
    val delAvTidligereUtbetaling: Boolean,
) : Periode<LocalDate>

fun BeregningsresultatBoutgifter.tilDto(revurderFra: LocalDate?): BeregningsresultatBoutgifterDto =
    BeregningsresultatBoutgifterDto(
        perioder =
            filtrerFraOgMed(revurderFra)
                .perioder
                .map { it.tilDto() },
        skalBrukeDetaljertVisning = skalBrukeDetaljertVisning(),
    )

private fun BeregningsresultatBoutgifter.filtrerFraOgMed(dato: LocalDate?): BeregningsresultatBoutgifter {
    if (dato == null) {
        return this
    }
    return BeregningsresultatBoutgifter(perioder.filter { it.tom >= dato })
}

private fun BeregningsresultatBoutgifter.skalBrukeDetaljertVisning(): Boolean =
    perioder.any {
        it.grunnlag.utgifter.keys
            .contains(TypeBoutgift.UTGIFTER_OVERNATTING)
    }

fun BeregningsresultatForLøpendeMåned.tilDto(): BeregningsresultatForPeriodeDto =
    BeregningsresultatForPeriodeDto(
        fom = grunnlag.fom,
        tom = grunnlag.tom,
        stønadsbeløp = stønadsbeløp,
        utbetalingsdato = grunnlag.utbetalingsdato,
        sumUtgifter = summerUtgifter(),
        utgifter = grunnlag.utgifter.values.flatten(),
        målgruppe = grunnlag.målgruppe,
        aktivitet = grunnlag.aktivitet,
        makssatsBekreftet = grunnlag.makssatsBekreftet,
        delAvTidligereUtbetaling = delAvTidligereUtbetaling,
    )
