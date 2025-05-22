package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.splitPerÅr
import no.nav.tilleggsstonader.sak.util.sisteDagenILøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import java.time.LocalDate

object LæremidlerVedtaksperiodeUtil {
    /**
     * Vedtaksperiode deles i ulike år då nytt år betyr ny termine og ikke skal utbetales direkte
     * For å innvilge høst og vår i 2 ulike perioder og der vårterminen får en ny sats
     */
    fun List<VedtaksperiodeBeregning>.splitVedtaksperiodePerÅr(): List<VedtaksperiodeInnenforÅr> =
        this
            .map { it.splitPerÅr { fom, tom -> VedtaksperiodeInnenforÅr(fom, tom) } }
            .flatten()
}

data class VedtaksperiodeInnenforÅr(
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Periode<LocalDate> {
    init {
        validatePeriode()
        require(fom.year == tom.year) {
            "Kan ikke være 2 ulike år (${fom.year}, ${tom.year}})"
        }
    }
}

/**
 * Tydligere at en vedtaksperiode er delt sånn at den skal være innenfor en [LøpendeMåned]
 */
data class VedtaksperiodeInnenforLøpendeMåned(
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Periode<LocalDate> {
    init {
        validatePeriode()
        require(tom <= fom.sisteDagenILøpendeMåned()) {
            "${this::class.simpleName} må være innenfor en løpende måned"
        }
    }
}
