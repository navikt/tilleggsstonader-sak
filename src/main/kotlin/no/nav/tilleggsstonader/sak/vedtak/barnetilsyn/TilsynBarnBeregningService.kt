package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.kontrakter.felles.erSortert
import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.kontrakter.felles.splitPerMåned
import no.nav.tilleggsstonader.libs.utils.VirkedagerProvider
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DatoperiodeMedAktivitetsdager
import org.apache.commons.lang3.ObjectUtils.min
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.util.UUID

/**
 * Stønaden dekker 64% av utgifterne til barnetilsyn
 */
private val DEKNINGSGRAD = BigDecimal("0.64")
private val SNITT_ANTALL_VIRKEDAGER_PER_MÅNED = BigDecimal("21.67")

@Service
class TilsynBarnBeregningService() {

    // Hva burde denne ta inn? Hva burde bli sendt inn i beregningscontroller?
    data class PeriodeMedDager(
        // val type: AktivitetType,
        override val fom: LocalDate,
        override val tom: LocalDate,
        val dager: Int,
    ) : Periode<LocalDate> {
        init {
            validatePeriode()
        }
    }

    fun beregn(
        stønadsperioder: List<Stønadsperiode>,
        utgifterPerBarn: Map<UUID, List<Utgift>>,
        aktiviteter: List<PeriodeMedDager> = emptyList(),
    ): BeregningsresultatTilsynBarnDto {
        validerPerioder(stønadsperioder, utgifterPerBarn)

        val beregningsgrunnlag = lagBeregningsgrunnlagPerMåned(stønadsperioder, utgifterPerBarn, aktiviteter)
        val perioder = beregn(beregningsgrunnlag)

        return BeregningsresultatTilsynBarnDto(perioder)
    }

    private fun beregn(beregningsgrunnlag: List<Beregningsgrunnlag>): List<Beregningsresultat> {
        return beregningsgrunnlag.map {
            val dagsats = beregnDagsats(it)
            Beregningsresultat(
                dagsats = dagsats,
                månedsbeløp = månedsbeløp(dagsats, it),
                grunnlag = it,
            )
        }
    }

    private fun månedsbeløp(
        dagsats: BigDecimal,
        beregningsgrunnlag: Beregningsgrunnlag,
    ) = dagsats.multiply(beregningsgrunnlag.antallDagerTotal.toBigDecimal()).setScale(0, RoundingMode.HALF_UP).toInt()

    /**
     * Divide trenger en scale som gir antall desimaler på resultatet fra divideringen
     * Sånn sett blir `setScale(2, RoundingMode.HALF_UP)` etteråt unødvendig
     * Tar likevel med den for å gjøre det tydelig at resultatet skal maks ha 2 desimaler
     */
    private fun beregnDagsats(grunnlag: Beregningsgrunnlag): BigDecimal {
        val utgifter = grunnlag.utgifterTotal.toBigDecimal()
        val utgifterSomDekkes = utgifter.multiply(DEKNINGSGRAD).setScale(0, RoundingMode.HALF_UP).toInt()
        val satsjusterteUtgifter = minOf(utgifterSomDekkes, grunnlag.makssats).toBigDecimal()
        return satsjusterteUtgifter.divide(SNITT_ANTALL_VIRKEDAGER_PER_MÅNED, 2, RoundingMode.HALF_UP)
            .setScale(2, RoundingMode.HALF_UP)
    }

    fun beregnHelePeriode(
        stønadsperioder: List<Stønadsperiode>,
        aktiviteter: List<PeriodeMedDager>,
    ): Int {
        val stønadsperioderPerUke = stønadsperioderTilUker(stønadsperioder)
        val aktiviteterPerUke = aktivitetsdagerTilUke(aktiviteter)

        val test = stønadsperioderPerUke.mapValues { (uke, periode) ->
            val maksAntallDagerDenneUka = min(periode.sumOf { it.dager }, 5)

            val test2 =
                aktiviteterPerUke[uke]?.sumOf { aktivitet -> aktivitet.sumOf { it.dager } } ?: 0 //TODO sjekk over denne

//            if(test2 == null) {
//                maksAntallDagerDenneUka
//            } else {
            min(maksAntallDagerDenneUka, test2)
//            }
        }

        return test.values.sum()
    }

    private fun lagBeregningsgrunnlagPerMåned(
        stønadsperioder: List<Stønadsperiode>,
        utgifterPerBarn: Map<UUID, List<Utgift>>,
        aktiviteter: List<PeriodeMedDager>,
    ): List<Beregningsgrunnlag> {
        val stønadsperioderPerMåned = stønadsperioder.tilÅrMåneder()
        val utgifterPerMåned = tilUtgifterPerMåned(utgifterPerBarn)
        val aktiviteterPerMåned = splittAktiviteterPåMåned(aktiviteter)

        return stønadsperioderPerMåned.entries.mapNotNull { (måned, stønadsperioder) ->
            utgifterPerMåned[måned]?.let { utgifter ->
                val antallBarn = utgifter.map { it.barnId }.toSet().size
                val makssats = finnMakssats(måned, antallBarn)
                Beregningsgrunnlag(
                    måned = måned,
                    makssats = makssats,
                    stønadsperioder = stønadsperioder,
                    utgifter = utgifter,
                    antallDagerTotal = antallDager(stønadsperioder, aktiviteterPerMåned[måned]),
                    utgifterTotal = utgifter.sumOf { it.utgift },
                    antallBarn = antallBarn,
                )
            }
        }
    }

    private fun finnMakssats(måned: YearMonth, antallBarn: Int): Int {
        val sats = satser.find { måned in it.fom..it.tom } ?: error("Finner ikke satser for $måned")
        val satsa = when {
            antallBarn == 1 -> sats.beløp[1]
            antallBarn == 2 -> sats.beløp[2]
            antallBarn > 2 -> sats.beløp[3]
            else -> null
        } ?: error("Kan ikke håndtere satser for antallBarn=$antallBarn")
        return satsa
    }

    private fun antallDagerIPeriode(aktivitet: PeriodeMedDager): Int {
        val antallHverdager = aktivitet.alleDatoer().count { it.dayOfWeek !== DayOfWeek.SATURDAY && it.dayOfWeek !== DayOfWeek.SUNDAY }

        return if (aktivitet.dager == 5) {
            antallHverdager
        } else {
            beregnAntallDager(antallHverdager, aktivitet.dager)
        }
    }

    fun antallHverdager(perioder: List<Stønadsperiode>): Int {
        return perioder.sumOf { it.alleDatoer().count { it.dayOfWeek !== DayOfWeek.SATURDAY && it.dayOfWeek !== DayOfWeek.SUNDAY } }
    }

    fun antallDager2(
        aktiviteter: List<PeriodeMedDager>
    ): Map<YearMonth, Int> {
        val aktiviteterPerMånedÅr = splittAktiviteterPåMåned(aktiviteter)
        return aktiviteterPerMånedÅr.mapValues{ (mnd, perioder) -> perioder.sumOf{ antallDagerIPeriode(it) }}
    }

    fun antallDager(
        stønadsperioder: List<Stønadsperiode>, aktiviteter: List<PeriodeMedDager>?
    ): Int {
        val antallUkedagerIStønadsperiode = stønadsperioder.sumOf { stønadsperiode ->
            stønadsperiode.alleDatoer().count { !VirkedagerProvider.erHelgEllerHelligdag(it) }
        }

        if (aktiviteter.isNullOrEmpty()) {
            return antallUkedagerIStønadsperiode
        }

        // Antar at alle aktiviteter sendt inn er av riktig type

        //val beregnetAntallDager = aktiviteter.sumOf { antallDagerIPeriode(it) }

        //return min(beregnetAntallDager, antallUkedagerIStønadsperiode)
        val stønadsperioderUker = stønadsperioderTilUker(stønadsperioder)
        val aktivitetUker = aktivitetsdagerTilUke(aktiviteter)

        // make stønadsperioderUker into Map<Uke, Int>


        val test = stønadsperioderUker.mapValues { (uke, periode) ->
            val maksAntallDagerDenneUka = min(periode.sumOf { it.dager }, 5)

            val test2 =
                aktivitetUker[uke]?.sumOf { aktivitet -> aktivitet.sumOf { it.dager } }

            if (test2 == null) {
                maksAntallDagerDenneUka
            } else {
                min(maksAntallDagerDenneUka, test2)
            }
        }

        return test.values.sum()
    }

    private fun DatoperiodeMedAktivitetsdager.snitt(annen: DatoperiodeMedAktivitetsdager): DatoperiodeMedAktivitetsdager? {
        return if (!overlapper(annen)) {
            null
        } else if (this == annen) {
            this
        } else {
            DatoperiodeMedAktivitetsdager(
                maxOf(fom, annen.fom), minOf(tom, annen.tom), aktivitetsdager
            )
        }
    }


    private fun beregnAntallDager(antallHverdager: Int, aktivitetsdager: Int): Int {
        val antallUker = antallHverdager.toBigDecimal().divide(BigDecimal(5))
        val antallHeleUker = antallUker.setScale(0, RoundingMode.FLOOR)

        // Utregning av gjenstående dager, kan gjøres i et men oppdelt for leslighet nå
        val desimalDelAvAntallUker =
            antallUker.subtract(antallHeleUker) // evt. fant dette på nettet: antallUker.remainder(BigDecimal.ONE)
        val antallEkstraDager = desimalDelAvAntallUker.multiply(BigDecimal(10)).divide(BigDecimal(2))

        return antallHeleUker.multiply(BigDecimal(aktivitetsdager))
            .plus(min(antallEkstraDager, BigDecimal(aktivitetsdager))).setScale(0, RoundingMode.FLOOR).toInt()
    }

    /**
     * Vi skal hådtere
     * 01.08 - 08.08,
     * 20.08 - 31.08
     * Er det enklast å returnere List<Pair<YearMonth, AntallDager>> som man sen grupperer per årmåned ?
     *
     * Del opp utgiftsperioder i atomiske deler (mnd).
     * Eksempel: 1stk UtgiftsperiodeDto fra januar til mars deles opp i 3:
     * listOf(UtgiftsMåned(jan), UtgiftsMåned(feb), UtgiftsMåned(mars))
     */
    fun List<Stønadsperiode>.tilÅrMåneder(): Map<YearMonth, List<Stønadsperiode>> {
        return this.flatMap { stønadsperiode ->
            stønadsperiode.splitPerMåned { måned, periode ->
                periode.copy(
                    fom = maxOf(periode.fom, måned.atDay(1)),
                    tom = minOf(periode.tom, måned.atEndOfMonth()),
                )
            }
        }.groupBy({ it.first }, { it.second })
    }

    /**
     * Splitter utgifter per måned
     */
    private fun tilUtgifterPerMåned(
        utgifterPerBarn: Map<UUID, List<Utgift>>,
    ): Map<YearMonth, List<UtgiftBarn>> {
        return utgifterPerBarn.entries.flatMap { (barnId, utgifter) ->
            utgifter.flatMap { utgift -> utgift.splitPerMåned { _, periode -> UtgiftBarn(barnId, periode.utgift) } }
        }.groupBy({ it.first }, { it.second })
    }

    fun splittAktiviteterPåMåned(aktiviteter: List<PeriodeMedDager>): Map<YearMonth, List<PeriodeMedDager>> {
        return aktiviteter.flatMap { periode ->
            periode.splitPerMåned { måned, periode ->
                periode.copy(
                    fom = maxOf(periode.fom, måned.atDay(1)),
                    tom = minOf(periode.tom, måned.atEndOfMonth()),
                )
            }
        }.groupBy({ it.first }, { it.second })
    }

    private fun validerPerioder(
        stønadsperioder: List<Stønadsperiode>,
        utgifter: Map<UUID, List<Utgift>>,
    ) {
        validerStønadsperioder(stønadsperioder)
        validerUtgifter(utgifter)
    }

    private fun validerStønadsperioder(stønadsperioder: List<Stønadsperiode>) {
        feilHvis(stønadsperioder.isEmpty()) {
            "Stønadsperioder mangler"
        }
        feilHvisIkke(stønadsperioder.erSortert()) {
            "Stønadsperioder er ikke sortert"
        }
        feilHvis(stønadsperioder.overlapper()) {
            "Stønadsperioder overlapper"
        }
    }

    private fun validerUtgifter(utgifter: Map<UUID, List<Utgift>>) {
        feilHvis(utgifter.values.flatten().isEmpty()) {
            "Utgiftsperioder mangler"
        }
        utgifter.entries.forEach { (_, utgifterForBarn) ->
            feilHvisIkke(utgifterForBarn.erSortert()) {
                "Utgiftsperioder er ikke sortert"
            }
            feilHvis(utgifterForBarn.overlapper()) {
                "Utgiftsperioder overlapper"
            }

            val ikkePositivUtgift = utgifterForBarn.firstOrNull { it.utgift < 1 }?.utgift
            feilHvis(ikkePositivUtgift != null) {
                "Utgiftsperioder inneholder ugyldig verdi: $ikkePositivUtgift"
            }
        }
    }

    fun <P : Periode<LocalDate>, VAL> P.splitPerUke(value: (uke: Uke, periode: P) -> VAL): Map<Uke, List<VAL>> {
        val perioder = mutableMapOf<Uke, MutableList<VAL>>()
        var startOfWeek: LocalDate

        if (this.fom.dayOfWeek == DayOfWeek.SATURDAY || this.fom.dayOfWeek == DayOfWeek.SUNDAY) {
            startOfWeek = this.fom.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        } else {
            startOfWeek = this.fom.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        }

        while (startOfWeek <= this.tom) {
            val endOfWeek = startOfWeek.plusDays(4)
            //.takeIf { it.isBefore(this.tom) }
            //?: this.tom // Adjust end date to the end of the period or end of the week
            val uke = Uke(startOfWeek, endOfWeek)
            val periode = perioder.getOrPut(uke) { mutableListOf() }
            periode.add(value(uke, this))
            startOfWeek = startOfWeek.plusWeeks(1)
        }

        return perioder
    }

    fun stønadsperioderTilUker(perioder: List<Stønadsperiode>): Map<Uke, List<PeriodeMedDager>> {
        return perioder.map {
            it.splitPerUke { uke, periode ->
                val fom = maxOf(uke.fom, periode.fom)
                val tom = minOf(uke.tom, periode.tom)
                PeriodeMedDager(
                    fom = fom, tom = tom, dager = antallDagerIPeriodeInklusiv(fom, tom)
                )
            }
        }.flatMap { it.entries }.groupBy({ it.key }, { it.value }).mapValues { it.value.flatten() }
    }

    fun aktivitetsdagerTilUke(perioder: List<PeriodeMedDager>): Map<Uke, List<List<PeriodeMedDager>>> {
        return perioder.map {
            it.splitPerUke { uke, periode ->
                val fom = maxOf(uke.fom, periode.fom)
                val tom = minOf(uke.tom, periode.tom)

                periode.copy(
                    fom = fom, tom = tom, dager = min(it.dager, antallDagerIPeriodeInklusiv(fom, tom))
                )
            }
        }.flatMap { it.entries }.groupBy({ it.key }, { it.value })
//            .mapValues { it.value.flatten() }
    }

    fun antallDagerIPeriodeInklusiv(fom: LocalDate, tom: LocalDate): Int {
        return fom.datesUntil(tom).count().toInt() + 1
    }

    data class Uke(
        override val fom: LocalDate,
        override val tom: LocalDate,
    ) : Periode<LocalDate>
}
