package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.splitPerMåned
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

object VedtaksperiodeBeregningUtil {
    /**
     * Deler opp vedtaksperiode i atomiske deler (mnd).
     *
     * Eksempel 1:
     * listOf(Periode(fom = 01.01.24, tom=31.03.24)) deles opp i 3:
     * jan -> listOf(Periode(jan), feb -> Periode(feb), mar -> Periode(mars))
     *
     * Eksempel 2:
     * listOf(Periode(fom = 01.01.24, tom=10.01.24), Periode(fom = 20.01.24, tom=31.01.24)) deles opp i 2 innenfor samme måned:
     * jan -> listOf(Periode(fom = 01.01.24, tom = 10.01.24), Periode(fom = 20.01.24, tom = 31.01.24))
     */
    fun List<VedtaksperiodeBeregning>.tilÅrMåned(): Map<YearMonth, List<VedtaksperiodeBeregning>> =
        this
            .flatMap { periode ->
                periode.splitPerMåned { måned, periode ->
                    periode.copy(
                        fom = maxOf(periode.fom, måned.atDay(1)),
                        tom = minOf(periode.tom, måned.atEndOfMonth()),
                    )
                }
            }.groupBy({ it.first }, { it.second })
            .mapValues { it.value.sorted() }

    /**
     * Metoden finner mandagen i nærmeste arbeidsuke
     * Dersom datoen er man-fre vil metoden returnere mandag samme uke
     * Er datoen lør eller søn returneres mandagen uken etter
     */
    private fun LocalDate.nærmesteRelevateMandag(): LocalDate =
        if (this.dayOfWeek == DayOfWeek.SATURDAY || this.dayOfWeek == DayOfWeek.SUNDAY) {
            this.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        } else {
            this.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        }

    /**
     * Splitter en periode opp i uker (kun hverdager inkludert)
     * Tar inn en funksjon for å beregne hvor mange dager man ønsker å telle i uken
     * enten alle eller begrense til antall aktivitetsdager
     */
    fun <P : Periode<LocalDate>> P.splitPerUke(antallDager: (fom: LocalDate, tom: LocalDate) -> Int): Map<Uke, PeriodeMedDager> {
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
     * Splitter en vedtaksperiode opp i uker (kun hverdager inkludert)
     * Antall dager i uken er oppad begrenset til antall dager i vedtaksperioden som er innenfor uken
     */
    fun VedtaksperiodeBeregning.tilUke(): Map<Uke, PeriodeMedDager> =
        this.splitPerUke { fom, tom ->
            antallDagerIPeriodeInklusiv(fom, tom)
        }

    fun antallDagerIPeriodeInklusiv(
        fom: LocalDate,
        tom: LocalDate,
    ): Int = ChronoUnit.DAYS.between(fom, tom).toInt() + 1

    fun <P> List<P>.brukPerioderFraOgMedTidligsteEndring(
        tidligsteEndring: LocalDate?,
    ): List<P> where P : Periode<LocalDate>, P : KopierPeriode<P> =
        tidligsteEndring?.let {
            this.splitFra(tidligsteEndring).filter { it.fom >= tidligsteEndring }
        } ?: this

    fun <P> List<P>.splitFra(dato: LocalDate?): List<P> where P : Periode<LocalDate>, P : KopierPeriode<P> {
        if (dato == null) return this
        return this.flatMap {
            if (it.fom < dato && dato <= it.tom) {
                listOf(
                    it.medPeriode(fom = it.fom, tom = dato.minusDays(1)),
                    it.medPeriode(fom = dato, tom = it.tom),
                )
            } else {
                listOf(it)
            }
        }
    }
}

data class Uke(
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Periode<LocalDate>

data class PeriodeMedDager(
    override val fom: LocalDate,
    override val tom: LocalDate,
    var antallDager: Int,
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }
}
