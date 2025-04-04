package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.DEKNINGSGRAD_TILSYN_BARN
import no.nav.tilleggsstonader.sak.vedtak.domain.Avslag
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksdata
import java.time.LocalDate

data class MakssatsDvh(
    val makssats: Int?,
    val beløpErBegrensetAvMakssats: Boolean?,
) {
    companion object {
        fun finnMakssatsDvh(
            andelTilkjentYtelse: AndelTilkjentYtelse,
            vedtaksdata: Vedtaksdata,
        ): MakssatsDvh =
            when (vedtaksdata) {
                is Avslag, is VedtakLæremidler -> ikkeRelevant

                is InnvilgelseEllerOpphørTilsynBarn ->
                    finnMakssatsTilsynBarn(
                        vedtaksdata = vedtaksdata,
                        andelFom = andelTilkjentYtelse.fom,
                    )

                is InnvilgelseBoutgifter ->
                    finnMakssatsBoutgifter(
                        vedtaksdata = vedtaksdata,
                        andelFom = andelTilkjentYtelse.fom,
                        andelsbeløp = andelTilkjentYtelse.beløp,
                    )
            }

        private val ikkeRelevant = MakssatsDvh(makssats = null, beløpErBegrensetAvMakssats = null)

        private fun finnMakssatsTilsynBarn(
            vedtaksdata: InnvilgelseEllerOpphørTilsynBarn,
            andelFom: LocalDate,
        ): MakssatsDvh {
            val beregningsgrunnlagSomGjelder =
                vedtaksdata.beregningsresultat.perioder
                    .find { it.grunnlag.måned == andelFom.toYearMonth() }
                    ?.grunnlag
                    ?: error("Skal ha beregningsgrunnlag når andel eksisterer")

            val erBeløpBegrensetAvMakssats =
                beregningsgrunnlagSomGjelder.let {
                    val utgifterSomDekkes =
                        (beregningsgrunnlagSomGjelder.utgifterTotal.toBigDecimal()).multiply(
                            DEKNINGSGRAD_TILSYN_BARN,
                        )
                    utgifterSomDekkes > beregningsgrunnlagSomGjelder.makssats.toBigDecimal()
                }

            return MakssatsDvh(
                makssats = beregningsgrunnlagSomGjelder.makssats,
                beløpErBegrensetAvMakssats = erBeløpBegrensetAvMakssats,
            )
        }

        private fun finnMakssatsBoutgifter(
            vedtaksdata: InnvilgelseEllerOpphørBoutgifter,
            andelFom: LocalDate,
            andelsbeløp: Int,
        ): MakssatsDvh {
            val beregningsgrunnlagSomGjelder =
                vedtaksdata.beregningsresultat.perioder
                    .find { it.fom.toYearMonth() == andelFom.toYearMonth() }
                    ?.grunnlag
                    ?: error("Skal ha beregningsgrunnlag når andel eksisterer")

            val makssats = beregningsgrunnlagSomGjelder.makssats

            return MakssatsDvh(makssats = makssats, beløpErBegrensetAvMakssats = andelsbeløp == makssats)
        }
    }
}
