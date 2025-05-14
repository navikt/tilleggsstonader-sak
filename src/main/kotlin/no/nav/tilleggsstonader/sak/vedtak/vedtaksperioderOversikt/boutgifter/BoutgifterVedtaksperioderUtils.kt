package no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt.boutgifter

import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.UtgiftBeregningBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift

object BoutgifterVedtaksperioderUtils {
    fun finnVedtaksperioderMedLøpendeUtgifter(
        vedtak: InnvilgelseEllerOpphørBoutgifter,
    ): List<BoLøpendeUtgift> {
        return vedtak.beregningsresultat.perioder
            .filter { it.grunnlag.utgifter.containsKey(TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG) }
            .map { resultatForMåned ->
                BoLøpendeUtgift(
                    fom = resultatForMåned.fom,
                    tom = resultatForMåned.tom,
                    aktivitet = resultatForMåned.grunnlag.aktivitet,
                    målgruppe = resultatForMåned.grunnlag.målgruppe,
                    utgift = summerLøpendeUtgifterBo(
                        resultatForMåned.grunnlag.utgifter,
                    ),
                    stønad = resultatForMåned.stønadsbeløp,
                )
            }
    }

    private fun summerLøpendeUtgifterBo(utgifter: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>>): Int {
        return utgifter.flatMap { (type, liste) ->
            liste.filter { type == TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG || type == TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER }
        }.sumOf { it.utgift }
    }
}