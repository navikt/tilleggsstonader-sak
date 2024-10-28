package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.splitPerMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningPeriode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

private val PROSENT_50 = BigDecimal(0.5)
private val PROSENTGRENSE_HALV_SATS = 50

class LæremidlerBeregningService {
    /**
     * Beregning av læremidler er foreløpig kun basert på antakelser.
     * Nå beregnes det med hele måneder, splittet i månedsskifte, men dette er ikke avklart som korrekt.
     * Det er ikke tatt hensyn til begrensninger på semester og maks antall måneder i et år som skal dekkes.
     */

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

    fun finnBeløpForStudieprosent(sats: Int, studieprosent: Int): Int {
        if (studieprosent <= PROSENTGRENSE_HALV_SATS) {
            return BigDecimal(sats).multiply(PROSENT_50).setScale(0, RoundingMode.HALF_UP).toInt()
        }
        return sats
    }
}
