package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.math.BigDecimal
import java.math.RoundingMode

object LæremidlerBeregnBeløpUtil {

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
