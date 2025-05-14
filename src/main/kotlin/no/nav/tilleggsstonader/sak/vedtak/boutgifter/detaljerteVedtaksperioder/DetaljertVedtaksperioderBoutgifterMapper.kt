package no.nav.tilleggsstonader.sak.vedtak.boutgifter.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.UtgiftBeregningBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift

object DetaljertVedtaksperioderBoutgifterMapper {
    fun InnvilgelseEllerOpphørBoutgifter.finnDetaljerteVedtaksperioder(): List<DetaljertVedtaksperiodeBoutgifter> {
        val overnatting = finnVedtaksperioderMedOvernattingUtgifter(this)
        val løpende = finnVedtaksperioderMedLøpendeUtgifter(this)

        return (overnatting + løpende).sorted()
    }

    private fun finnVedtaksperioderMedOvernattingUtgifter(
        vedtak: InnvilgelseEllerOpphørBoutgifter,
    ): List<DetaljertVedtaksperiodeBoutgifter> {
        val relevantePerioder =
            vedtak.beregningsresultat.perioder
                .filter { it.grunnlag.utgifter.containsKey(TypeBoutgift.UTGIFTER_OVERNATTING) }

        return relevantePerioder.map { it.mapBeregningsresultatMndTilDetaljertVedtaksperiode() }
    }

    private fun BeregningsresultatForLøpendeMåned.mapBeregningsresultatMndTilDetaljertVedtaksperiode(): DetaljertVedtaksperiodeBoutgifter {
        val utgifterTilOvernatting =
            beregnAndelAvUtgifterSomDekkes(
                this.grunnlag.utgifter,
                this.grunnlag.makssats,
            )

        return DetaljertVedtaksperiodeBoutgifter(
            fom = this.fom,
            tom = this.tom,
            aktivitet = this.grunnlag.aktivitet,
            målgruppe = this.grunnlag.målgruppe,
            utgifterTilOvernatting = utgifterTilOvernatting,
            totalUtgiftMåned = utgifterTilOvernatting.sumOf { it.utgift }, // kan droppes om vi ikke bruker den
            stønadsbeløpMnd = utgifterTilOvernatting.sumOf { it.beløpSomDekkes }, // kan droppes om vi ikke bruker den
            erLøpendeUtgift = false,
        )
    }

    private fun finnVedtaksperioderMedLøpendeUtgifter(vedtak: InnvilgelseEllerOpphørBoutgifter): List<DetaljertVedtaksperiodeBoutgifter> =
        vedtak.beregningsresultat.perioder
            .filter { it.grunnlag.utgifter.containsKey(TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG) }
            .map { resultatForMåned ->
                DetaljertVedtaksperiodeBoutgifter(
                    fom = resultatForMåned.fom,
                    tom = resultatForMåned.tom,
                    aktivitet = resultatForMåned.grunnlag.aktivitet,
                    målgruppe = resultatForMåned.grunnlag.målgruppe,
                    totalUtgiftMåned =
                        summerLøpendeUtgifterBo(
                            resultatForMåned.grunnlag.utgifter,
                        ),
                    stønadsbeløpMnd = resultatForMåned.stønadsbeløp,
                    erLøpendeUtgift = true,
                )
            }.sorterOgMergeSammenhengende()

    private fun beregnAndelAvUtgifterSomDekkes(
        utgifter: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>>,
        makssats: Int,
    ): List<UtgiftTilOvernatting> {
        val utgifterOvernatting = utgifter[TypeBoutgift.UTGIFTER_OVERNATTING] ?: error("Burde ikke være mulig")

        var sumForMåned = 0

        return utgifterOvernatting.map { utgift ->
            val beløpSomDekkes = minOf(utgift.utgift, makssats - sumForMåned)
            sumForMåned += beløpSomDekkes

            UtgiftTilOvernatting(
                fom = utgift.fom,
                tom = utgift.tom,
                utgift = utgift.utgift,
                beløpSomDekkes = beløpSomDekkes,
            )
        }
    }

    private fun summerLøpendeUtgifterBo(utgifter: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>>): Int =
        utgifter
            .flatMap { (type, liste) ->
                liste.filter { type == TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG || type == TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER }
            }.sumOf { it.utgift }
}
