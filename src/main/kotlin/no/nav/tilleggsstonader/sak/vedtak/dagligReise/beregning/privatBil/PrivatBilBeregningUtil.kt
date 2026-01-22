package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.periode.beregnSnitt
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.PeriodeMedAntallDager
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode

fun finnRelevantVedtaksperiodeForUke(
    uke: Datoperiode,
    vedtaksperioder: List<Vedtaksperiode>,
): Vedtaksperiode? {
    val sammenslåtteVedtaksperioder =
        vedtaksperioder
            .sorted()
            .mergeSammenhengende { v1, v2 -> v1.erSammenhengendeMedLikMålgruppeOgTypeAktivitet(v2) }
    val vedtaksperioderSomOverlapperUke = sammenslåtteVedtaksperioder.mapNotNull { it.beregnSnitt(uke) }

    if (vedtaksperioderSomOverlapperUke.size > 1) {
        håndterFlereVedtaksperioderInnenforEnUke(uke, sammenslåtteVedtaksperioder)
    }

    return vedtaksperioderSomOverlapperUke.firstOrNull()
}

fun Datoperiode.tilpassUkeTilVedtaksperiode(vedtaksperiode: Vedtaksperiode): PeriodeMedAntallDager? {
    val snitt = this.beregnSnitt(vedtaksperiode) ?: return null

    return PeriodeMedAntallDager(
        fom = snitt.fom,
        tom = snitt.tom,
    )
}

private fun håndterFlereVedtaksperioderInnenforEnUke(
    uke: Datoperiode,
    vedtaksperioder: List<Vedtaksperiode>,
) {
    val antallMålgrupper = vedtaksperioder.map { it.målgruppe }.distinct().size
    brukerfeilHvis(antallMålgrupper > 1) {
        "Beregningen klarer ikke å håndtere flere ulike målgrupper innenfor samme uke. Gjelder uke ${uke.formatertPeriodeNorskFormat()}"
    }

    val antallTypeAktiviteter = vedtaksperioder.map { it.typeAktivitet }.distinct().size
    brukerfeilHvis(antallTypeAktiviteter > 1) {
        "Beregningen klarer ikke å håndtere flere ulike aktivitetstyper innenfor samme uke. Gjelder uke ${uke.formatertPeriodeNorskFormat()}"
    }

    brukerfeil(
        "Beregning klarer ikke å håndtere opphold mellom to vedtaksperioder innenfor en uke. Gjelder uke ${uke.formatertPeriodeNorskFormat()}",
    )
}

data class AntallDagerSomDekkes(
    val antallDager: Int,
    val inkludererHelg: Boolean,
)

fun finnAntallDagerSomDekkes(
    uke: PeriodeMedAntallDager,
    reisedagerPerUke: Int,
): AntallDagerSomDekkes {
    var antallDager: Int
    var antallDagerInkludererHelg: Boolean

    val totaltAntallDagerIUke = uke.antallHverdager + uke.antallHelgedager

    if (reisedagerPerUke <= uke.antallHverdager) {
        antallDager = reisedagerPerUke
        antallDagerInkludererHelg = false
    } else if (reisedagerPerUke <= totaltAntallDagerIUke) {
        antallDager = reisedagerPerUke
        antallDagerInkludererHelg = true
    } else {
        antallDager = totaltAntallDagerIUke
        antallDagerInkludererHelg = uke.antallHelgedager > 0
    }

    return AntallDagerSomDekkes(
        antallDager = antallDager,
        inkludererHelg = antallDagerInkludererHelg,
    )
}
