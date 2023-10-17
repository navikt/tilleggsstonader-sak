package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.UUID

@Service
class TilsynBarnBeregningService {

    // Hva burde denne ta inn? Hva burde bli sendt inn i beregningscontroller?
    fun beregn(
        stønadsperioder: List<Stønadsperiode>,
        utgifterPerBarn: Map<UUID, List<Utgift>>,
    ): BeregningsresultatTilsynBarnDto {
        validerPerioder(stønadsperioder, utgifterPerBarn)

        val beregningsgrunnlag = lagBeregningsgrunnlag(stønadsperioder, utgifterPerBarn)
        val perioder = beregn(beregningsgrunnlag)

        return BeregningsresultatTilsynBarnDto(perioder)
    }

    private fun beregn(beregningsgrunnlag: List<Beregningsgrunnlag>): List<Beregningsresultat> {
        return beregningsgrunnlag.map {
            Beregningsresultat(
                fom = it.måned,
                tom = it.måned,
                makssats = 100, // TODO
                dagsats = 100, // TODO
                grunnlag = it
            )
        }.mergeSammenhengende(::skalSlåSammen)
    }

    private fun skalSlåSammen(
        b1: Beregningsresultat,
        b2: Beregningsresultat
    ) = b1.tom.plusMonths(1) == b2.fom &&
            b1.makssats == b2.makssats &&
            b1.dagsats == b2.dagsats &&
            b1.grunnlag == b2.grunnlag

    private fun lagBeregningsgrunnlag(
        stønadsperioder: List<Stønadsperiode>,
        utgifterPerBarn: Map<UUID, List<Utgift>>
    ): List<Beregningsgrunnlag> {
        val stønadsdagerPerÅrMåned = stønadsperioder.tilÅrMåneder()

        val stønadsperiode = stønadsperiode(stønadsperioder)

        val utgifterPerMåned = getUtgifterPerMåned(utgifterPerBarn, stønadsperiode)

        return stønadsdagerPerÅrMåned.entries.mapNotNull { (måned, antallDager) ->
            utgifterPerMåned[måned]?.let {
                Beregningsgrunnlag(
                    måned = måned,
                    antallDager = antallDager,
                    utgifter = it
                )
            }
        }
    }

    private fun stønadsperiode(stønadsperioder: List<Stønadsperiode>): ClosedRange<YearMonth> {
        val førsteStønadsmåned = YearMonth.from(stønadsperioder.first().fom)
        val sisteStønadsmåned = YearMonth.from(stønadsperioder.last().tom)
        return førsteStønadsmåned..sisteStønadsmåned
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
    fun List<Stønadsperiode>.tilÅrMåneder(): Map<YearMonth, Int> {
        return this.flatMap { it.splitPerMåned { 10 } } // TODO beregn dager i periode
            .groupBy { it.first }
            .mapValues { (_, values) -> values.sumOf { it.second } }
    }

    /**
     * Splitter utgifter per måned
     */
    private fun getUtgifterPerMåned(
        utgifterPerBarn: Map<UUID, List<Utgift>>,
        stønadsperiode: ClosedRange<YearMonth>
    ): Map<YearMonth, List<UtgiftForBarn>> {
        return utgifterPerBarn.entries.flatMap { (barnId, utgifter) ->
            utgifter.flatMap { utgift -> utgift.splitPerMåned { UtgiftForBarn(barnId, it.utgift) } }
        }.groupBy({ it.first }, { it.second })
            .filterKeys { it in stønadsperiode }
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

            // TODO burde vi validere utgiftsperioder vs stønadsperioder?
            //  fom >= stønadsperiode
            //  tom <= støndsperiode?
        }
    }
}
