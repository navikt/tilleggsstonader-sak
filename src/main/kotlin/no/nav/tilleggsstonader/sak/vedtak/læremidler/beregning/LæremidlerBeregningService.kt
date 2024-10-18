package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.splitPerMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningPeriode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import java.math.BigDecimal
import java.time.YearMonth
import kotlin.math.ceil

class LæremidlerBeregningService {

    fun beregn(periode: BeregningPeriode): BeregningsresultatLæremidler {
        val beregningsgrunnlagPerMåned = periode.splitPerMåned { måned, periode ->
            val beregningsGrunnlag = lagBeregningsGrunnlag(periode, måned)
            BeregningsresultatForMåned(
                beløp = finnBeløpForStudieprosent(beregningsGrunnlag.sats, beregningsGrunnlag.studieprosent),
                grunnlag = lagBeregningsGrunnlag(periode, måned),
            )
        }.map { it.second }
        return BeregningsresultatLæremidler(
            perioder = beregningsgrunnlagPerMåned,
        )
    }

    fun lagBeregningsGrunnlag(periode: BeregningPeriode, måned: YearMonth): Beregningsgrunnlag {
        return Beregningsgrunnlag(
            måned = måned,
            studienivå = periode.studienivå,
            studieprosent = periode.studieprosent,
            sats = finnSatsForStudienivå(måned, periode.studienivå),
        )
    }

    fun finnBeløpForStudieprosent(sats: Int, studieprosent: Int): BigDecimal {
        if (studieprosent <= 50) {
            return BigDecimal(ceil(sats * 0.5))
        }
        return BigDecimal(sats)
    }
}
