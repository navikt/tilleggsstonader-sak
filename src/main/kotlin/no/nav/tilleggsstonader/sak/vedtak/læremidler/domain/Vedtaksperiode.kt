package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.periode.avkortFraOgMed
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import java.time.LocalDate

data class Vedtaksperiode(
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }
}

fun avkortVedtaksperiodeVedOpphør(forrigeVedtak: Vedtak, revurderFra: LocalDate): List<Vedtaksperiode> {
    var avkortetVedtaksperiodeListe: List<Vedtaksperiode> = emptyList()

    fun avkortVedtaksperiode(vedtaksperiode: Vedtaksperiode) {
        val avkortetDatoPeriode = Datoperiode(vedtaksperiode.fom, vedtaksperiode.tom).avkortFraOgMed(revurderFra.minusDays(1))
        if (avkortetDatoPeriode !== null) {
            avkortetVedtaksperiodeListe = avkortetVedtaksperiodeListe.plus(
                Vedtaksperiode(fom = avkortetDatoPeriode.fom, tom = avkortetDatoPeriode.tom),

            )
        }
    }

    when (forrigeVedtak.type) {
        TypeVedtak.INNVILGELSE -> {
            val forrigeVedtakInnvilgelse = forrigeVedtak.withTypeOrThrow<InnvilgelseLæremidler>()
            forrigeVedtakInnvilgelse.data.vedtaksperioder.forEach {
                avkortVedtaksperiode(it)
            }
        }
        TypeVedtak.OPPHØR -> {
            val forrigeVedtakInnvilgelse = forrigeVedtak.withTypeOrThrow<OpphørLæremidler>()
            forrigeVedtakInnvilgelse.data.vedtaksperioder.forEach {
                avkortVedtaksperiode(it)
            }
        }
        TypeVedtak.AVSLAG -> {
            TODO()
        }
    }

    return avkortetVedtaksperiodeListe
}
