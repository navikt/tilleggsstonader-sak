package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.kontrakter.felles.splitPerÅr
import no.nav.tilleggsstonader.sak.util.lørdagEllerSøndag
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterVedtaksperiodeUtil.sisteDagenILøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate

object BoutgifterVedtaksperiodeUtil {
    /**
     * Vedtaksperiode deles i ulike år då nytt år betyr ny termine og ikke skal utbetales direkte
     * For å innvilge høst og vår i 2 ulike perioder og der vårterminen får en ny sats
     */
    fun List<VedtaksperiodeBeregning>.splitVedtaksperiodePerÅr(): List<VedtaksperiodeInnenforÅr> =
        this
            .map {
                it.splitPerÅr { fom, tom ->
                    VedtaksperiodeInnenforÅr(
                        fom = fom,
                        tom = tom,
                        målgruppe = it.målgruppe,
                        aktivitet = it.aktivitet,
                    )
                }
            }.flatten()

    /**
     * Splitter en periode i løpende måneder. Løpende måned er fra dagens dato og en måned frem i tiden.
     * eks 05.01.2024-29.02.24 blir listOf( P(fom=05.01.2024,tom=04.02.2024), P(fom=05.02.2024,tom=29.02.2024) )
     */
    fun <P : Periode<LocalDate>, VAL : Periode<LocalDate>> P.splitPerLøpendeMåneder(
        medNyPeriode: (fom: LocalDate, tom: LocalDate) -> VAL,
    ): List<VAL> {
        val perioder = mutableListOf<VAL>()
        var gjeldendeFom = fom
        while (gjeldendeFom <= tom) {
            val nyTom = minOf(gjeldendeFom.sisteDagenILøpendeMåned(), tom)

            val nyPeriode = medNyPeriode(gjeldendeFom, nyTom)
            if (nyPeriode.harDatoerIUkedager()) {
                perioder.add(nyPeriode)
            }

            gjeldendeFom = nyTom.plusDays(1)
        }
        return perioder
    }

    fun LocalDate.sisteDagenILøpendeMåned(): LocalDate = this.plusMonths(1).minusDays(1)

    private fun <P : Periode<LocalDate>> P.harDatoerIUkedager(): Boolean =
        this
            .alleDatoer()
            .any { dato -> !dato.lørdagEllerSøndag() }
}

data class VedtaksperiodeInnenforÅr(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppe: MålgruppeType,
    val aktivitet: AktivitetType,
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
    val målgruppe: MålgruppeType,
    val aktivitet: AktivitetType,
) : Periode<LocalDate> {
    init {
        validatePeriode()
        require(tom <= fom.sisteDagenILøpendeMåned()) {
            "${this::class.simpleName} må være innenfor en løpende måned"
        }
    }
}
