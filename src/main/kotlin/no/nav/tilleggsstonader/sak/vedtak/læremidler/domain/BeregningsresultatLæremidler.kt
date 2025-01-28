package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.periode.AvkortResult
import no.nav.tilleggsstonader.kontrakter.periode.avkortFraOgMed
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate

data class BeregningsresultatLæremidler(
    val perioder: List<BeregningsresultatForMåned>,
) {
    fun filtrerFraOgMed(dato: LocalDate?): BeregningsresultatLæremidler {
        if (dato == null) return this
        return BeregningsresultatLæremidler(perioder.filter { it.grunnlag.tom >= dato })
    }
}

data class BeregningsresultatForMåned(
    val beløp: Int,
    val grunnlag: Beregningsgrunnlag,
) : Periode<LocalDate>,
    KopierPeriode<BeregningsresultatForMåned> {
    @get:JsonIgnore
    override val fom: LocalDate get() = grunnlag.fom

    @get:JsonIgnore
    override val tom: LocalDate get() = grunnlag.tom

    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): BeregningsresultatForMåned = this.copy(grunnlag = this.grunnlag.copy(fom = fom, tom = tom))

    fun medKorrigertUtbetalingsdato(utbetalingsdato: LocalDate): BeregningsresultatForMåned =
        this.copy(grunnlag = grunnlag.copy(utbetalingsdato = utbetalingsdato))
}

data class Beregningsgrunnlag(
    val fom: LocalDate,
    val tom: LocalDate,
    val utbetalingsdato: LocalDate,
    val studienivå: Studienivå,
    val studieprosent: Int,
    val sats: Int,
    val satsBekreftet: Boolean,
    val målgruppe: MålgruppeType,
)

fun avkortBeregningsresultatVedOpphør(
    forrigeVedtak: GeneriskVedtak<out InnvilgelseEllerOpphørLæremidler>,
    revurderFra: LocalDate,
): AvkortResult<BeregningsresultatForMåned> =
    forrigeVedtak
        .data
        .beregningsresultat
        .perioder
        .avkortFraOgMed(revurderFra.minusDays(1)) { periode, nyttTom ->
            periode.copy(grunnlag = periode.grunnlag.copy(tom = nyttTom))
        }
