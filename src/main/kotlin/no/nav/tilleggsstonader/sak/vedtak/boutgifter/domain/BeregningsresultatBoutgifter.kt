package no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.periode.avkortPerioderFør
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.UtgiftBeregningBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate
import kotlin.math.min

data class BeregningsresultatBoutgifter(
    val perioder: List<BeregningsresultatForLøpendeMåned>,
) {
    fun filtrerFraOgMed(dato: LocalDate?): BeregningsresultatBoutgifter {
        if (dato == null) {
            return this
        }
        return BeregningsresultatBoutgifter(perioder.avkortPerioderFør(dato))
    }
}

data class BeregningsresultatForLøpendeMåned(
    val grunnlag: Beregningsgrunnlag,
    val delAvTidligereUtbetaling: Boolean = false,
) : Periode<LocalDate>,
    KopierPeriode<BeregningsresultatForLøpendeMåned> {
    @get:JsonIgnore
    override val fom: LocalDate get() = grunnlag.fom

    @get:JsonIgnore
    override val tom: LocalDate get() = grunnlag.tom

    @get:JsonProperty("stønadsbeløp")
    val stønadsbeløp get() = beregnBeløp()

    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): BeregningsresultatForLøpendeMåned = this.copy(grunnlag = this.grunnlag.copy(fom = fom, tom = tom))

    fun medKorrigertUtbetalingsdato(utbetalingsdato: LocalDate): BeregningsresultatForLøpendeMåned =
        this.copy(grunnlag = grunnlag.copy(utbetalingsdato = utbetalingsdato))

    fun markerSomDelAvTidligereUtbetaling(delAvTidligereUtbetaling: Boolean) =
        this.copy(delAvTidligereUtbetaling = delAvTidligereUtbetaling)

    private fun beregnBeløp(): Int {
        val summerteUtgifter =
            grunnlag.utgifter.values
                .flatten()
                .sumOf { it.utgift }
        return min(summerteUtgifter, grunnlag.makssats)
    }
}

data class Beregningsgrunnlag(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val utbetalingsdato: LocalDate,
    val utgifter: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>>,
    val makssats: Int,
    val makssatsBekreftet: Boolean,
    val målgruppe: MålgruppeType,
    val aktivitet: AktivitetType,
) : Periode<LocalDate>

// fun avkortBeregningsresultatVedOpphør(
//    forrigeVedtak: GeneriskVedtak<out InnvilgelseEllerOpphørBoutgifter>,
//    revurderFra: LocalDate,
// ): AvkortResult<BeregningsresultatForMåned> =
//    forrigeVedtak
//        .data
//        .beregningsresultat
//        .perioder
//        .avkortFraOgMed(revurderFra.minusDays(1)) { periode, nyttTom ->
//            periode.copy(grunnlag = periode.grunnlag.copy(tom = nyttTom))
//        }
