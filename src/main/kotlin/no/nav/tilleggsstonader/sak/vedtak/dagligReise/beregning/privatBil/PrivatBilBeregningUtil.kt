package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.periode.beregnSnitt
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.antallHelgedagerIPeriodeInklusiv
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.antallHverdagerIPeriodeInklusiv
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

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
        kastFeilVedFlereVedtaksperioderInnenforEnUke(uke, sammenslåtteVedtaksperioder)
    }

    return vedtaksperioderSomOverlapperUke.firstOrNull()
}

fun Datoperiode.finnAntallDagerIUkeInnenforVedtaksperiode(vedtaksperiode: Vedtaksperiode): PeriodeMedAntallDager? {
    val snitt = this.beregnSnitt(vedtaksperiode) ?: return null

    return PeriodeMedAntallDager(
        fom = snitt.fom,
        tom = snitt.tom,
    )
}

private fun kastFeilVedFlereVedtaksperioderInnenforEnUke(
    uke: Datoperiode,
    vedtaksperioder: List<Vedtaksperiode>,
): Nothing {
    val antallMålgrupper = vedtaksperioder.map { it.målgruppe }.distinct().size
    brukerfeilHvis(antallMålgrupper > 1) {
        "Beregningen klarer ikke å håndtere flere ulike målgrupper innenfor samme uke. " +
            "Gjelder uke ${uke.formatertPeriodeNorskFormat()}"
    }

    val antallTypeAktiviteter = vedtaksperioder.map { it.typeAktivitet }.distinct().size
    brukerfeilHvis(antallTypeAktiviteter > 1) {
        "Beregningen klarer ikke å håndtere flere ulike aktivitetsvarianter innenfor samme uke. " +
            "Gjelder uke ${uke.formatertPeriodeNorskFormat()}"
    }

    brukerfeil(
        "Beregning klarer ikke å håndtere opphold mellom to vedtaksperioder innenfor en uke. " +
            "Gjelder uke ${uke.formatertPeriodeNorskFormat()}. " +
            "Splitt reiseperioden slik at den matcher vedtaksperiodene",
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
    val totaltAntallDagerIUke = uke.antallHverdager + uke.antallHelgedager

    if (reisedagerPerUke <= uke.antallHverdager) {
        return AntallDagerSomDekkes(
            antallDager = reisedagerPerUke,
            inkludererHelg = false,
        )
    }

    if (reisedagerPerUke <= totaltAntallDagerIUke) {
        return AntallDagerSomDekkes(
            antallDager = reisedagerPerUke,
            inkludererHelg = true,
        )
    }

    return AntallDagerSomDekkes(
        antallDager = totaltAntallDagerIUke,
        inkludererHelg = uke.antallHelgedager > 0,
    )
}

fun <P : Periode<LocalDate>> P.splitPerUkeMedHelg(): List<Datoperiode> {
    val uker = mutableListOf<Datoperiode>()

    var startOfWeek = this.fom

    while (startOfWeek <= this.tom) {
        val nærmesteSøndagFremITid = startOfWeek.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val endOfWeek: LocalDate = minOf(nærmesteSøndagFremITid, this.tom)

        uker.add(
            Datoperiode(
                fom = startOfWeek,
                tom = endOfWeek,
            ),
        )

        startOfWeek = endOfWeek.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
    }

    return uker
}

data class PeriodeMedAntallDager(
    override val fom: LocalDate,
    override val tom: LocalDate,
    var antallHverdager: Int,
    var antallHelgedager: Int,
) : Periode<LocalDate>,
    KopierPeriode<PeriodeMedAntallDager> {
    init {
        validatePeriode()
    }

    constructor(fom: LocalDate, tom: LocalDate) : this(
        fom = fom,
        tom = tom,
        antallHverdager = antallHverdagerIPeriodeInklusiv(fom = fom, tom = tom),
        antallHelgedager = antallHelgedagerIPeriodeInklusiv(fom = fom, tom = tom),
    )

    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): PeriodeMedAntallDager = this.copy(fom = fom, tom = tom)
}
