package no.nav.tilleggsstonader.sak.vedtak.læremidler

import java.math.BigDecimal
import java.time.YearMonth
import kotlin.math.ceil

class LæremidlerBeregningService {

    fun beregn(studienivå: Studienivå, studieprosent: Int, måned: YearMonth): BigDecimal {
        val sats = finnSats(måned, studienivå)
        return if (studieprosent <= 50) BigDecimal(ceil(sats * 0.5)) else BigDecimal(sats)
    }
}

enum class Studienivå {
    VIDEREGÅENDE,
    HØYERE_UTDANNING,
}