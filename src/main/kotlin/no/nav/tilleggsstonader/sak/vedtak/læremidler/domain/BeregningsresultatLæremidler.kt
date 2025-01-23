package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
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
)

data class Beregningsgrunnlag(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val utbetalingsdato: LocalDate,
    val studienivå: Studienivå,
    val studieprosent: Int,
    val sats: Int,
    val satsBekreftet: Boolean,
    val målgruppe: MålgruppeType,
) : Periode<LocalDate>

fun kuttePerioderVedOpphør(forrigeVedtak: Vedtak, revurderFra: LocalDate): List<BeregningsresultatForMåned> {
    var kuttedePerioder: List<BeregningsresultatForMåned> = emptyList()

    when (forrigeVedtak.type) {
        TypeVedtak.INNVILGELSE -> {
            val forrigeVedtakInnvilgelse = forrigeVedtak.withTypeOrThrow<InnvilgelseLæremidler>()
            forrigeVedtakInnvilgelse.data.beregningsresultat.perioder.forEach {
                if (it.grunnlag.tom < revurderFra) {
                    kuttedePerioder = kuttedePerioder.plus(it)
                } else if (it.grunnlag.fom < revurderFra) {
                    kuttedePerioder.plus(it.copy(grunnlag = it.grunnlag.copy(tom = revurderFra.minusDays(1))))
                }
            }
        }
        TypeVedtak.OPPHØR -> {
            val forrigeVedtakOpphør = forrigeVedtak.withTypeOrThrow<OpphørLæremidler>()
            forrigeVedtakOpphør.data.beregningsresultat.perioder.forEach {
                if (it.grunnlag.tom < revurderFra) {
                    kuttedePerioder = kuttedePerioder.plus(it)
                } else if (it.grunnlag.fom < revurderFra) {
                    kuttedePerioder = kuttedePerioder.plus(it.copy(grunnlag = it.grunnlag.copy(tom = revurderFra.minusDays(1))))
                }
            }
        }
        TypeVedtak.AVSLAG -> TODO()
    }
    return kuttedePerioder
}
