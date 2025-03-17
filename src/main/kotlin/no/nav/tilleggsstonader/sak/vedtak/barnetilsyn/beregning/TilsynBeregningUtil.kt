package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.kontrakter.felles.splitPerMåned
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Aktivitet
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.UtgiftBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.PeriodeMedDager
import no.nav.tilleggsstonader.sak.vedtak.domain.Uke
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.antallDagerIPeriodeInklusiv
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.splitPerUke
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.min

object TilsynBeregningUtil {
    /**
     * Splitter opp utgifter slik at de blir fordelt per måned og grupperer de etter måned.
     * Resultatet gir en map med måned som key og en liste av utgifter.
     * Listen med utgifter består av utgiften knyttet til et barn for gitt måned.
     */
    fun Map<BarnId, List<UtgiftBeregning>>.tilÅrMåned(): Map<YearMonth, List<UtgiftBarn>> =
        this.entries
            .flatMap { (barnId, utgifter) ->
                utgifter.flatMap { utgift -> utgift.splitPerMåned { _, periode -> UtgiftBarn(barnId, periode.utgift) } }
            }.groupBy({ it.first }, { it.second })

    /**
     * Deler opp aktiviteter i atomiske deler (mnd) og grupperer aktivitetene per AktivitetType.
     */
    fun List<Aktivitet>.tilAktiviteterPerMånedPerType(): Map<YearMonth, Map<AktivitetType, List<Aktivitet>>> =
        this
            .flatMap { stønadsperiode ->
                stønadsperiode.splitPerMåned { måned, periode ->
                    periode.copy(
                        fom = maxOf(periode.fom, måned.atDay(1)),
                        tom = minOf(periode.tom, måned.atEndOfMonth()),
                    )
                }
            }.groupBy({ it.first }, { it.second })
            .mapValues { it.value.groupBy { it.type } }

    /**
     * Splitter en liste av aktiviteter opp i uker (kun hverdager inkludert)
     * Antall dager i uken er oppad begrenset til det lavest av antall aktivitetsdager eller antall
     * dager i aktivitetsperioden som er innenfor uken
     */
    fun List<Aktivitet>.tilDagerPerUke(): Map<Uke, List<PeriodeMedDager>> =
        this
            .map { aktivitet ->
                aktivitet.splitPerUke { fom, tom ->
                    min(aktivitet.aktivitetsdager, antallDagerIPeriodeInklusiv(fom, tom))
                }
            }.flatMap { it.entries }
            .groupBy({ it.key }, { it.value })
            .mapValues { it.value.sorted() }

    fun Map<BarnId, List<UtgiftBeregning>>.brukPerioderFraOgMedRevurderFraMåned(
        revurderFra: LocalDate?,
    ): Map<BarnId, List<UtgiftBeregning>> {
        val revurderFraMåned = revurderFra?.toYearMonth() ?: return this

        return this
            .mapValues { (_, utgifter) ->
                utgifter.splitFraRevurderFra(revurderFra).filter { it.fom >= revurderFraMåned }
            }.filterValues { it.isNotEmpty() }
    }
}
