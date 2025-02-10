package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.splitPerMåned
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Aktivitet
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.UtgiftBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.min

object TilsynBeregningUtilsFelles {
    /**
     * Deler opp perioder i atomiske deler (mnd).
     *
     * Eksempel 1:
     * listOf(StønadsperiodeDto(fom = 01.01.24, tom=31.03.24)) deles opp i 3:
     * jan -> listOf(StønadsperiodeDto(jan), feb -> StønadsperiodeDto(feb), mar -> StønadsperiodeDto(mars))
     *
     * Eksempel 2:
     * listOf(StønadsperiodeDto(fom = 01.01.24, tom=10.01.24), StønadsperiodeDto(fom = 20.01.24, tom=31.01.24)) deles opp i 2 innenfor samme måned:
     * jan -> listOf(StønadsperiodeDto(fom = 01.01.24, tom = 10.01.24), StønadsperiodeDto(fom = 20.01.24, tom = 31.01.24))
     */
    fun <P> List<P>.tilÅrMåned(): Map<YearMonth, List<P>> where P : Periode<LocalDate>, P : KopierPeriode<P> =
        this
            .flatMap { stønadsperiode ->
                stønadsperiode.splitPerMåned { måned, periode ->
                    periode.medPeriode(
                        fom = maxOf(periode.fom, måned.atDay(1)),
                        tom = minOf(periode.tom, måned.atEndOfMonth()),
                    )
                }
            }.groupBy({ it.first }, { it.second })
            .mapValues { it.value.sorted() }

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
     * Splitter en periode opp i uker (kun hverdager inkludert)
     * Antall dager i uken er oppad begrenset til antall dager i perioden som er innenfor uken
     */
    fun <P : Periode<LocalDate>> P.tilUke(): Map<Uke, PeriodeMedDager> =
        this.splitPerUke { fom, tom ->
            antallDagerIPeriodeInklusiv(fom, tom)
        }

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

    private fun antallDagerIPeriodeInklusiv(
        fom: LocalDate,
        tom: LocalDate,
    ): Int = ChronoUnit.DAYS.between(fom, tom).toInt() + 1

    fun <P> List<P>.brukPerioderFraOgMedRevurderFra(revurderFra: LocalDate?): List<P> where P : Periode<LocalDate>, P : KopierPeriode<P> =
        revurderFra?.let {
            this.splitFraRevurderFra(revurderFra).filter { it.fom >= revurderFra }
        } ?: this

    fun <P> List<P>.splitFraRevurderFra(revurderFra: LocalDate?): List<P> where P : Periode<LocalDate>, P : KopierPeriode<P> {
        if (revurderFra == null) return this
        return this.flatMap {
            if (it.fom < revurderFra && revurderFra <= it.tom) {
                listOf(
                    it.medPeriode(fom = it.fom, tom = revurderFra.minusDays(1)),
                    it.medPeriode(fom = revurderFra, tom = it.tom),
                )
            } else {
                listOf(it)
            }
        }
    }

    /**
     * Dersom man har satt revurderFra så skal man kun beregne perioder fra og med den måneden
     * Hvis vi eks innvilget 1000kr for 1-31 august, så mappes hele beløpet til 1 august.
     * Dvs det lages en andel som har fom-tom 1-1 aug
     * Når man revurderer fra midten på måneden og eks skal endre målgruppe eller aktivitetsdager,
     * så har man allerede utbetalt 500kr for 1-14 august, men hele beløpet er ført på 1 aug.
     * For at beregningen då skal bli riktig må man ha med grunnlaget til hele måneden og beregne det på nytt, sånn at man får en ny periode som er
     * 1-14 aug, 500kr, 15-30 aug 700kr.
     */
    fun List<Beregningsgrunnlag>.brukBeregningsgrunnlagFraOgMedRevurderFra(revurderFra: LocalDate?): List<Beregningsgrunnlag> {
        val revurderFraMåned = revurderFra?.toYearMonth() ?: return this

        return this.filter { it.måned >= revurderFraMåned }
    }

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
