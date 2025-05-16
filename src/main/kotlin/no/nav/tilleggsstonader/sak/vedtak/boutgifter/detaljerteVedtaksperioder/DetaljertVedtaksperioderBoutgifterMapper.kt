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

        return relevantePerioder.map { it.mapBeregningsresultatMndOvernatting() }
    }

    private fun finnVedtaksperioderMedLøpendeUtgifter(vedtak: InnvilgelseEllerOpphørBoutgifter): List<DetaljertVedtaksperiodeBoutgifter> {
        val relevantePerioder =
            vedtak.beregningsresultat.perioder
                .filter { it.grunnlag.utgifter.containsKey(TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG) }

        return relevantePerioder
            .map { it.mapBeregningsresultatMndLøpendeUtgift() }
            .sorterOgMergeSammenhengende()
    }

    private fun BeregningsresultatForLøpendeMåned.mapBeregningsresultatMndOvernatting(): DetaljertVedtaksperiodeBoutgifter {
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
            totalUtgiftMåned = utgifterTilOvernatting.sumOf { it.utgift },
            stønadsbeløpMnd = utgifterTilOvernatting.sumOf { it.beløpSomDekkes }, // bruk stønadsbeløp direkte
            erLøpendeUtgift = false,
        )
    }

    private fun BeregningsresultatForLøpendeMåned.mapBeregningsresultatMndLøpendeUtgift(): DetaljertVedtaksperiodeBoutgifter {
        val sumLøpendeUtgifter =
            summerLøpendeUtgifterBo(
                this.grunnlag.utgifter,
            )

        return DetaljertVedtaksperiodeBoutgifter(
            fom = this.fom,
            tom = this.tom,
            aktivitet = this.grunnlag.aktivitet,
            målgruppe = this.grunnlag.målgruppe,
            totalUtgiftMåned = sumLøpendeUtgifter,
            stønadsbeløpMnd = minOf(this.grunnlag.makssats, sumLøpendeUtgifter),
            erLøpendeUtgift = true,
        )
    }

    private fun beregnAndelAvUtgifterSomDekkes(
        utgifter: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>>,
        makssats: Int,
    ): List<UtgiftTilOvernatting> {
        val utgifterOvernatting = utgifter[TypeBoutgift.UTGIFTER_OVERNATTING] ?: error("Burde ikke være mulig")

        val sumLøpendeUtgifter =
            utgifter
                .filterKeys {
                    it == TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG || it == TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER
                }.values
                .flatten()
                .sumOf { it.utgift }

        // Todo - kommentere hvorfor vi må ta hensyn til løpende utgifter
        var alleredeSummerteUtgifter = sumLøpendeUtgifter

        return utgifterOvernatting
            .sorted()
            .map { utgift ->
                val beløpSomDekkes = minOf(utgift.utgift, makssats - alleredeSummerteUtgifter)
                alleredeSummerteUtgifter += beløpSomDekkes

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
