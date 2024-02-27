package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.kontrakter.felles.erSortert
import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.kontrakter.felles.splitPerMåned
import no.nav.tilleggsstonader.libs.utils.VirkedagerProvider
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.tilSortertDto
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import java.util.UUID

/**
 * Stønaden dekker 64% av utgifterne til barnetilsyn
 */
private val DEKNINGSGRAD = BigDecimal("0.64")
private val SNITT_ANTALL_VIRKEDAGER_PER_MÅNED = BigDecimal("21.67")

@Service
class TilsynBarnBeregningService(private val stønadsperiodeRepository: StønadsperiodeRepository) {

    // Hva burde denne ta inn? Hva burde bli sendt inn i beregningscontroller?
    fun lagBeregningsgrunnlagOgBeregn(
        behandlingId: UUID,
        utgifterPerBarn: Map<UUID, List<Utgift>>,
    ): BeregningsresultatTilsynBarnDto {
        val stønadsperioder = stønadsperiodeRepository.findAllByBehandlingId(behandlingId).tilSortertDto()
        validerPerioder(stønadsperioder, utgifterPerBarn)

        val beregningsgrunnlag = lagBeregningsgrunnlagPerMåned(stønadsperioder, utgifterPerBarn)
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
    ) =
        dagsats.multiply(beregningsgrunnlag.antallDagerTotal.toBigDecimal())
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()

    /**
     * Divide trenger en scale som gir antall desimaler på resultatet fra divideringen
     * Sånn sett blir `setScale(2, RoundingMode.HALF_UP)` etteråt unødvendig
     * Tar likevel med den for å gjøre det tydelig at resultatet skal maks ha 2 desimaler
     */
    private fun beregnDagsats(grunnlag: Beregningsgrunnlag): BigDecimal {
        val utgifter = grunnlag.utgifterTotal.toBigDecimal()
        val utgifterSomDekkes = utgifter.multiply(DEKNINGSGRAD)
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()
        val satsjusterteUtgifter = minOf(utgifterSomDekkes, grunnlag.makssats).toBigDecimal()
        return satsjusterteUtgifter
            .divide(SNITT_ANTALL_VIRKEDAGER_PER_MÅNED, 2, RoundingMode.HALF_UP)
            .setScale(2, RoundingMode.HALF_UP)
    }

    private fun lagBeregningsgrunnlagPerMåned(
        stønadsperioder: List<StønadsperiodeDto>,
        utgifterPerBarn: Map<UUID, List<Utgift>>,
    ): List<Beregningsgrunnlag> {
        val stønadsperioderPerMåned = stønadsperioder.tilÅrMåneder()

        val utgifterPerMåned = tilUtgifterPerMåned(utgifterPerBarn)

        return stønadsperioderPerMåned.entries.mapNotNull { (måned, stønadsperioder) ->
            utgifterPerMåned[måned]?.let { utgifter ->
                val antallBarn = utgifter.map { it.barnId }.toSet().size
                val makssats = finnMakssats(måned, antallBarn)
                Beregningsgrunnlag(
                    måned = måned,
                    makssats = makssats,
                    stønadsperioder = stønadsperioder,
                    utgifter = utgifter,
                    antallDagerTotal = antallDager(stønadsperioder),
                    utgifterTotal = utgifter.sumOf { it.utgift },
                    antallBarn = antallBarn,
                )
            }
        }
    }

    private fun antallDager(stønadsperioder: List<StønadsperiodeDto>): Int {
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
    fun List<StønadsperiodeDto>.tilÅrMåneder(): Map<YearMonth, List<StønadsperiodeDto>> {
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
     */
    private fun tilUtgifterPerMåned(
        utgifterPerBarn: Map<UUID, List<Utgift>>,
    ): Map<YearMonth, List<UtgiftBarn>> {
        return utgifterPerBarn.entries.flatMap { (barnId, utgifter) ->
            utgifter.flatMap { utgift -> utgift.splitPerMåned { _, periode -> UtgiftBarn(barnId, periode.utgift) } }
        }.groupBy({ it.first }, { it.second })
    }

    private fun validerPerioder(
        stønadsperioder: List<StønadsperiodeDto>,
        utgifter: Map<UUID, List<Utgift>>,
    ) {
        validerStønadsperioder(stønadsperioder)
        validerUtgifter(utgifter)
    }

    private fun validerStønadsperioder(stønadsperioder: List<StønadsperiodeDto>) {
        feilHvis(stønadsperioder.isEmpty()) {
            "Stønadsperioder mangler"
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
