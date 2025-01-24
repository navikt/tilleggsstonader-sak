package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import java.math.BigDecimal
import java.math.RoundingMode

object LæremidlerBeregnBeløpUtil {

    private val PROSENT_50 = BigDecimal(0.5)
    private val PROSENTGRENSE_HALV_SATS = 50

    fun beregnBeløp(sats: Int, studieprosent: Int): Int {
        if (studieprosent <= PROSENTGRENSE_HALV_SATS) {
            return BigDecimal(sats)
                .multiply(PROSENT_50)
                .setScale(0, RoundingMode.HALF_UP)
                .toInt()
        }
        return sats
    }
}
