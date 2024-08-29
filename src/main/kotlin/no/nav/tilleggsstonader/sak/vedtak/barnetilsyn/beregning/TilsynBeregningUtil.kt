package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.splitPerMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.Utgift
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.UtgiftBarn
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import kotlin.math.min

object TilsynBeregningUtil {
    /**
     * Deler opp  stønadsperioder i atomiske deler (mnd).
     *
     * Eksempel 1:
     * listOf(StønadsperiodeDto(fom = 01.01.24, tom=31.03.24)) deles opp i 3:
     * jan -> listOf(StønadsperiodeDto(jan), feb -> StønadsperiodeDto(feb), mar -> StønadsperiodeDto(mars))
     *
     * Eksempel 2:
     * listOf(StønadsperiodeDto(fom = 01.01.24, tom=10.01.24), StønadsperiodeDto(fom = 20.01.24, tom=31.01.24)) deles opp i 2 innenfor samme måned:
     * jan -> listOf(StønadsperiodeDto(fom = 01.01.24, tom = 10.01.24), StønadsperiodeDto(fom = 20.01.24, tom = 31.01.24))
     */
    fun List<StønadsperiodeDto>.tilÅrMåned(): Map<YearMonth, List<StønadsperiodeDto>> {
        return this
            .flatMap { stønadsperiode ->
                stønadsperiode.splitPerMåned { måned, periode ->
                    periode.copy(
                        fom = maxOf(periode.fom, måned.atDay(1)),
                        tom = minOf(periode.tom, måned.atEndOfMonth()),
                    )
                }
            }
            .groupBy({ it.first }, { it.second })
    }

    /**
     * Splitter opp utgifter slik at de blir fordelt per måned og grupperer de etter måned.
     * Resultatet gir en map med måned som key og en liste av utgifter.
     * Listen med utgifter består av utgiften knyttet til et barn for gitt måned.
     */
    fun Map<UUID, List<Utgift>>.tilÅrMåned(): Map<YearMonth, List<UtgiftBarn>> {
        return this.entries.flatMap { (barnId, utgifter) ->
            utgifter.flatMap { utgift -> utgift.splitPerMåned { _, periode -> UtgiftBarn(barnId, periode.utgift) } }
        }.groupBy({ it.first }, { it.second })
    }

    /**
     * Deler opp aktiviteter i atomiske deler (mnd) og grupperer aktivitetene per AktivitetType.
     */
    fun List<Aktivitet>.tilAktiviteterPerMånedPerType(): Map<YearMonth, Map<AktivitetType, List<Aktivitet>>> {
        return this
            .flatMap { stønadsperiode ->
                stønadsperiode.splitPerMåned { måned, periode ->
                    periode.copy(
                        fom = maxOf(periode.fom, måned.atDay(1)),
                        tom = minOf(periode.tom, måned.atEndOfMonth()),
                    )
                }
            }
            .groupBy({ it.first }, { it.second }).mapValues { it.value.groupBy { it.type } }
    }

    /**
     * Metoden finner mandagen i nærmeste arbeidsuke
     * Dersom datoen er man-fre vil metoden returnere mandag samme uke
     * Er datoen lør eller søn returneres mandagen uken etter
     */
    private fun LocalDate.nærmesteRelevateMandag(): LocalDate {
        if (this.dayOfWeek == DayOfWeek.SATURDAY || this.dayOfWeek == DayOfWeek.SUNDAY) {
            return this.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        } else {
            return this.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        }
    }

    /**
     * Splitter en periode opp i uker (kun hverdager inkludert)
     * Tar inn en funksjon for å beregne hvor mange dager man ønsker å telle i uken
     * enten alle eller begrense til antall aktivitetsdager
     */
    private fun <P : Periode<LocalDate>> P.splitPerUke(antallDager: (fom: LocalDate, tom: LocalDate) -> Int): Map<Uke, PeriodeMedDager> {
        val periode = mutableMapOf<Uke, PeriodeMedDager>()
        var startOfWeek = this.fom.nærmesteRelevateMandag()

        while (startOfWeek <= this.tom) {
            val endOfWeek = startOfWeek.plusDays(4)
            val uke = Uke(startOfWeek, endOfWeek)
            val fom = maxOf(uke.fom, this.fom)
            val tom = minOf(uke.tom, this.tom)

            periode.getOrPut(uke) { PeriodeMedDager(fom, tom, antallDager(fom, tom)) }

            startOfWeek = startOfWeek.plusWeeks(1)
        }

        return periode
    }

    /**
     * Splitter en stønadsperiode opp i uker (kun hverdager inkludert)
     * Antall dager i uken er oppad begrenset til antall dager i stønadsperioden som er innenfor uken
     */
    fun StønadsperiodeDto.tilUke(): Map<Uke, PeriodeMedDager> {
        return this.splitPerUke { fom, tom ->
            antallDagerIPeriodeInklusiv(fom, tom)
        }
    }

    /**
     * Splitter en liste av aktiviteter opp i uker (kun hverdager inkludert)
     * Antall dager i uken er oppad begrenset til det lavest av antall aktivitetsdager eller antall
     * dager i aktivitetsperioden som er innenfor uken
     */
    fun List<Aktivitet>.tilDagerPerUke(): Map<Uke, List<PeriodeMedDager>> {
        return this.map { aktivitet ->
            aktivitet.splitPerUke { fom, tom ->
                min(aktivitet.aktivitetsdager, antallDagerIPeriodeInklusiv(fom, tom))
            }
        }.flatMap { it.entries }.groupBy({ it.key }, { it.value })
    }

    private fun antallDagerIPeriodeInklusiv(fom: LocalDate, tom: LocalDate): Int {
        return ChronoUnit.DAYS.between(fom, tom).toInt() + 1
    }
}

data class Uke(
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Periode<LocalDate>

data class PeriodeMedDager(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val antallDager: Int,
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }
}
