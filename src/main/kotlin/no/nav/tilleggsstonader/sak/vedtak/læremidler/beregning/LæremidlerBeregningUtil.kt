package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.splitPerÅr
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerPeriodeUtil.splitPerLøpendeMåneder
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

object LæremidlerBeregningUtil {

    /**
     * Splitter vedtaksperiode per år. Sånn at man får en periode for høstterminen og en for vårterminen
     * Dette for å kunne lage en periode for våren som ikke utbetales direkte, men når satsen for det nye året er satt.
     * Eks 2024-08-15 - 2025-06-20 blir 2024-08-15 - 2024-12-31 og 2025-01-01 - 2025-06-20
     */
    fun Periode<LocalDate>.delTilUtbetalingsPerioder(): List<UtbetalingPeriode> =
        splitPerÅr { fom, tom -> Vedtaksperiode(fom, tom) }
            .flatMap { periode ->
                periode.splitPerLøpendeMåneder { fom, tom ->
                    UtbetalingPeriode(
                        fom = fom,
                        tom = tom,
                        utbetalingsdato = periode.fom.datoEllerNesteMandagHvisLørdagEllerSøndag(),
                    )
                }
            }

    fun beregnBeløp(sats: Int, studieprosent: Int): Int {
        val PROSENT_50 = BigDecimal(0.5)
        val PROSENTGRENSE_HALV_SATS = 50

        if (studieprosent <= PROSENTGRENSE_HALV_SATS) {
            return BigDecimal(sats).multiply(PROSENT_50).setScale(0, RoundingMode.HALF_UP).toInt()
        }
        return sats
    }
}

data class MålgruppeOgAktivitet(
    val målgruppe: MålgruppeType,
    val aktivitet: AktivitetLæremidlerBeregningGrunnlag,
)
