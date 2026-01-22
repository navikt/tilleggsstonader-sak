package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.periode.beregnSnitt
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import java.time.LocalDate

fun antallHverdagerIPeriodeInklusiv(
    fom: LocalDate,
    tom: LocalDate,
): Int =
    generateSequence(fom) { it.plusDays(1) }
        .takeWhile { !it.isAfter(tom) }
        .count { it.dayOfWeek.value in 1..5 }

data class ReiseOgVedtaksperioderSnitt<P>(
    val justerteVedtaksperioder: List<Vedtaksperiode>,
    val justertReiseperiode: P,
) where P : Periode<LocalDate>, P : KopierPeriode<P>

fun <P> finnSnittMellomReiseOgVedtaksperioder(
    reise: P,
    vedtaksperioder: List<Vedtaksperiode>,
): ReiseOgVedtaksperioderSnitt<P> where P : Periode<LocalDate>, P : KopierPeriode<P> {
    val justerteVedtaksperioder = vedtaksperioder.mapNotNull { it.beregnSnitt(reise) }.sorted()
    return ReiseOgVedtaksperioderSnitt(
        justerteVedtaksperioder = justerteVedtaksperioder,
        justertReiseperiode =
            reise.medPeriode(
                fom = maxOf(justerteVedtaksperioder.first().fom, reise.fom),
                tom = minOf(justerteVedtaksperioder.last().tom, reise.tom),
            ),
    )
}