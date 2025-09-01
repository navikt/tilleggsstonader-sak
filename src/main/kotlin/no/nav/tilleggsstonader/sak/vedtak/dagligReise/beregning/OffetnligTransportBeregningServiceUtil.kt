package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.kontrakter.periode.beregnSnitt
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.UtgiftOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.antallDagerIPeriodeInklusiv
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.splitPerUke
import kotlin.math.min

data class ReiseOgVedtaksperioderSnitt(
    val justerteVedtaksperioder: List<Vedtaksperiode>,
    val justertReiseperiode: UtgiftOffentligTransport,
)

fun finnSnittMellomReiseOgVedtaksperioder(
    reise: UtgiftOffentligTransport,
    vedtaksperioder: List<Vedtaksperiode>,
): ReiseOgVedtaksperioderSnitt =
    ReiseOgVedtaksperioderSnitt(
        justerteVedtaksperioder = vedtaksperioder.mapNotNull { it.beregnSnitt(reise) },
        justertReiseperiode =
            reise.copy(
                fom = maxOf(vedtaksperioder.first().fom, reise.fom),
                tom = minOf(vedtaksperioder.last().tom, reise.tom),
            ),
    )

fun finnAntallSyvDagersPerioder(grunnlag: Beregningsgrunnlag): Int =
    kotlin.math.ceil(antallDagerIPeriodeInklusiv(grunnlag.fom, grunnlag.tom) / 7.0).toInt()

fun finnReisedagerIPeriode(
    vedtaksperiode: Vedtaksperiode,
    antallReisedagerPerUke: Int,
): Int =
    vedtaksperiode
        .splitPerUke { fom, tom ->
            min(antallReisedagerPerUke, antallDagerIPeriodeInklusiv(fom, tom))
        }.values
        .sumOf { it.antallDager }
