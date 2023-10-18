package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.kontrakter.felles.erSortert
import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.kontrakter.felles.splitPerMåned
import no.nav.tilleggsstonader.libs.utils.VirkedagerProvider
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import java.util.UUID

private val SATS_PROSENT = BigDecimal("0.64")
private val SNITT_ANTALL_DAGER_PER_MÅNED = BigDecimal("21.67")

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
                makssats = 100, // TODO trenger makssats
                dagsats = beregnDagsats(it),
                grunnlag = it,
            )
        }
    }

    /**
     * Divide trenger en scale som gir antall desimaler på resultatet fra divideringen
     * Sånn sett blir `setScale(2, RoundingMode.HALF_UP)` etteråt unødvendig
     * Tar likevel med den for å gjøre det tydelig at resultatet skal maks ha 2 desimaler
     */
    private fun beregnDagsats(grunnlag: Beregningsgrunnlag): BigDecimal {
        val utgifter = grunnlag.utgifterTotal.toBigDecimal()
        return utgifter.multiply(SATS_PROSENT)
            .divide(SNITT_ANTALL_DAGER_PER_MÅNED, 2, RoundingMode.HALF_UP)
            .setScale(2, RoundingMode.HALF_UP)
    }

    private fun lagBeregningsgrunnlag(
        stønadsperioder: List<Stønadsperiode>,
        utgifterPerBarn: Map<UUID, List<Utgift>>,
    ): List<Beregningsgrunnlag> {
        val stønadsperioderPerMåned = stønadsperioder.tilÅrMåneder()

        val utgifterPerMåned = tilUtgifterPerMåned(utgifterPerBarn)

        return stønadsperioderPerMåned.entries.mapNotNull { (måned, stønadsperioder) ->
            utgifterPerMåned[måned]?.let { utgifter ->
                Beregningsgrunnlag(
                    måned = måned,
                    stønadsperioder = stønadsperioder,
                    utgifter = utgifter,
                    antallDagerTotal = antallDager(stønadsperioder),
                    utgifterTotal = utgifter.sumOf { it.utgift },
                )
            }
        }
    }

    private fun antallDager(stønadsperioder: List<Stønadsperiode>): Int {
        return stønadsperioder.sumOf { stønadsperiode ->
            stønadsperiode.alleDatoer().count { !VirkedagerProvider.erHelgEllerHelligdag(it) }
        }
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
     * Splitter utgifter per måned
     * Filtrerer vekk måneder som ikke er en del av stønadsperioden då vi ikke trenger å
     */
    private fun tilUtgifterPerMåned(
        utgifterPerBarn: Map<UUID, List<Utgift>>,
    ): Map<YearMonth, List<UtgiftForBarn>> {
        return utgifterPerBarn.entries.flatMap { (barnId, utgifter) ->
            utgifter.flatMap { utgift -> utgift.splitPerMåned { _, periode -> UtgiftForBarn(barnId, periode.utgift) } }
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
}
