package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.DEKNINGSGRAD_TILSYN_BARN
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import java.time.LocalDate
import java.time.YearMonth

data class UtbetalingerDvh(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val type: AndelstypeDvh,
    val beløp: Int,
    val makssats: Int? = null,
    val beløpErBegrensetAvMakssats: Boolean? = null,
) {
    data class JsonWrapper(
        val utbetalinger: List<UtbetalingerDvh>,
    )

    companion object {
        fun fraDomene(
            ytelser: List<AndelTilkjentYtelse>,
            vedtak: Vedtak,
        ) = JsonWrapper(
            ytelser.filterNot { it.type == TypeAndel.UGYLDIG }.map { andelTilkjentYtelse ->

                val beregningsgrunnlag = andelTilkjentYtelse.finnGrunnlag(vedtak)

                UtbetalingerDvh(
                    fraOgMed = andelTilkjentYtelse.fom,
                    tilOgMed = andelTilkjentYtelse.tom,
                    type = AndelstypeDvh.fraDomene(andelTilkjentYtelse.type),
                    beløp = andelTilkjentYtelse.beløp,
                    makssats = beregningsgrunnlag?.makssats,
                    beløpErBegrensetAvMakssats = erBeløpBegrensetAvMakssats(beregningsgrunnlag),
                )
            },
        )

        private fun AndelTilkjentYtelse.finnGrunnlag(vedtak: Vedtak): Beregningsgrunnlag? =
            when (vedtak.data) {
                is AvslagLæremidler -> null
                is AvslagTilsynBarn -> null
                is InnvilgelseLæremidler -> null
                is InnvilgelseTilsynBarn -> vedtak.data.finnGrunnlag(this.fom.toYearMonth())
                is OpphørTilsynBarn -> vedtak.data.finnGrunnlag(this.fom.toYearMonth())
                is OpphørLæremidler -> null
                is InnvilgelseBoutgifter -> TODO("Utbetaling for boutgifter er ikke implementert enda")
            }

        private fun InnvilgelseTilsynBarn.finnGrunnlag(måned: YearMonth): Beregningsgrunnlag =
            this.beregningsresultat.perioder
                .find { it.grunnlag.måned == måned }
                ?.grunnlag
                ?: error("Skal ha beregningsgrunnlag hvis andeler eksisterer")

        private fun OpphørTilsynBarn.finnGrunnlag(måned: YearMonth): Beregningsgrunnlag =
            this.beregningsresultat.perioder
                .find { it.grunnlag.måned == måned }
                ?.grunnlag
                ?: error("Skal ha beregningsgrunnlag hvis andeler eksisterer")

        private fun erBeløpBegrensetAvMakssats(beregningsgrunnlag: Beregningsgrunnlag?): Boolean? =
            beregningsgrunnlag?.let {
                val utgifterSomDekkes =
                    (beregningsgrunnlag.utgifterTotal.toBigDecimal()).multiply(
                        DEKNINGSGRAD_TILSYN_BARN,
                    )
                utgifterSomDekkes > beregningsgrunnlag.makssats.toBigDecimal()
            }
    }
}

enum class AndelstypeDvh {
    TILSYN_BARN_ENSLIG_FORSØRGER,
    TILSYN_BARN_AAP,
    TILSYN_BARN_ETTERLATTE,

    LÆREMIDLER_ENSLIG_FORSØRGER,
    LÆREMIDLER_AAP,
    LÆREMIDLER_ETTERLATTE,

    BOUTGIFTER_AAP,
    BOUTGIFTER_ETTERLATTE,
    BOUTGIFTER_ENSLIG_FORSØRGER,

    ;

    companion object {
        fun fraDomene(typeAndel: TypeAndel) =
            when (typeAndel) {
                TypeAndel.TILSYN_BARN_ENSLIG_FORSØRGER -> TILSYN_BARN_ENSLIG_FORSØRGER
                TypeAndel.TILSYN_BARN_AAP -> TILSYN_BARN_AAP
                TypeAndel.TILSYN_BARN_ETTERLATTE -> TILSYN_BARN_ETTERLATTE
                TypeAndel.LÆREMIDLER_ENSLIG_FORSØRGER -> LÆREMIDLER_ENSLIG_FORSØRGER
                TypeAndel.LÆREMIDLER_AAP -> LÆREMIDLER_AAP
                TypeAndel.LÆREMIDLER_ETTERLATTE -> LÆREMIDLER_ETTERLATTE
                TypeAndel.BOUTGIFTER_AAP -> BOUTGIFTER_AAP
                TypeAndel.BOUTGIFTER_ENSLIG_FORSØRGER -> BOUTGIFTER_ENSLIG_FORSØRGER
                TypeAndel.BOUTGIFTER_ETTERLATTE -> BOUTGIFTER_ETTERLATTE
                TypeAndel.UGYLDIG -> throw Error("Trenger ikke statistikk på ugyldige betalinger")
            }
    }
}
