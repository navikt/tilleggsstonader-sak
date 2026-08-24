package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.domain.Avslag
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørPassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksdata
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.beregning.DEKNINGSGRAD_TILSYN_BARN
import java.time.LocalDate

data class MakssatsDvhUtil(
    val makssats: Int?,
    val beløpErBegrensetAvMakssats: Boolean?,
) {
    companion object {
        fun finnMakssats(
            andelTilkjentYtelse: AndelTilkjentYtelse,
            vedtaksdata: Vedtaksdata,
        ): MakssatsDvhUtil =
            when (vedtaksdata) {
                is Avslag, is VedtakLæremidler -> ikkeRelevant

                is InnvilgelseEllerOpphørPassAvBarn ->
                    finnMakssatsPassAvBarn(
                        vedtaksdata = vedtaksdata,
                        andelFom = andelTilkjentYtelse.fom,
                    )

                is InnvilgelseEllerOpphørBoutgifter ->
                    finnMakssatsBoutgifter(
                        vedtaksdata = vedtaksdata,
                        andelFom = andelTilkjentYtelse.fom,
                        andelsbeløp = andelTilkjentYtelse.beløp,
                    )

                is InnvilgelseEllerOpphørDagligReise -> ikkeRelevant
                is InnvilgelseEllerOpphørReiseTilSamling -> ikkeRelevant
            }

        private val ikkeRelevant = MakssatsDvhUtil(makssats = null, beløpErBegrensetAvMakssats = null)

        private fun finnMakssatsPassAvBarn(
            vedtaksdata: InnvilgelseEllerOpphørPassAvBarn,
            andelFom: LocalDate,
        ): MakssatsDvhUtil {
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

            return MakssatsDvhUtil(
                makssats = beregningsgrunnlagSomGjelder.makssats,
                beløpErBegrensetAvMakssats = erBeløpBegrensetAvMakssats,
            )
        }

        private fun finnMakssatsBoutgifter(
            vedtaksdata: InnvilgelseEllerOpphørBoutgifter,
            andelFom: LocalDate,
            andelsbeløp: Int,
        ): MakssatsDvhUtil {
            val beregningsgrunnlagSomGjelder =
                vedtaksdata.beregningsresultat.perioder
                    .find { it.fom.toYearMonth() == andelFom.toYearMonth() }
                    ?.grunnlag
                    ?: error("Skal ha beregningsgrunnlag når andel eksisterer")

            val makssats = beregningsgrunnlagSomGjelder.makssats

            return MakssatsDvhUtil(makssats = makssats, beløpErBegrensetAvMakssats = andelsbeløp == makssats)
        }
    }
}
