package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.util.lørdagEllerSøndag
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterVedtaksperiodeUtil.sisteDagenILøpendeMåned
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate

/**
 * Utbetalingperiode for løpende måned
 * Eks 5jan - 4feb
 *
 * @param tom er maks dato for vedtaksperiode inne i en løpende måned.
 * I de tilfeller man kun har en vedtaksperiode som gjelder 5 jan - 7 jan så vil tom= 7 jan.
 *
 * @param utbetalingsdato utbetalingsdato for når en utbetalingsperiode skal utbetales.
 * Eks hvis man innvilger jan-juni så skal man utbetale hele beløpet for fom i første utbetalingsperioden,
 * dvs 5 jan i tidligere eksemplet
 *
 */
data class UtbetalingPeriode(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppe: MålgruppeType,
    val aktivitet: AktivitetType,
    val utbetalingsdato: LocalDate,
) : Periode<LocalDate> {
    init {
        validatePeriode()
        require(tom <= fom.sisteDagenILøpendeMåned()) {
            "UtbetalingPeriode kan ikke løpe lengre enn en løpende måned"
        }
    }

//    constructor(
//        løpendeMåned: LøpendeMåned,
//        målgruppeOgAktivitet: MålgruppeOgAktivitet,
//    ) : this(
//        fom = løpendeMåned.fom,
//        tom = løpendeMåned.vedtaksperioder.maxOf { it.tom },
//        målgruppe = målgruppeOgAktivitet.målgruppe,
//        aktivitet = målgruppeOgAktivitet.aktivitet.type,
//        studienivå = målgruppeOgAktivitet.aktivitet.studienivå,
//        prosent = målgruppeOgAktivitet.aktivitet.prosent,
//        utbetalingsdato = løpendeMåned.utbetalingsdato,
//    )

    constructor(
        løpendeMåned: LøpendeMåned,
    ) : this(
        fom = løpendeMåned.fom,
        tom = løpendeMåned.vedtaksperioder.maxOf { it.tom },
        // TODO: Prioriter hvilken målgruppe som skal være gjeldende til økonomi hvis ulike målgrupper havner innenfor samme løpende måned
        målgruppe = løpendeMåned.vedtaksperioder.first().målgruppe,
        aktivitet = løpendeMåned.vedtaksperioder.first().aktivitet,
//        studienivå = målgruppeOgAktivitet.aktivitet.studienivå,
//        prosent = målgruppeOgAktivitet.aktivitet.prosent,
        utbetalingsdato = løpendeMåned.utbetalingsdato,
    )
}

data class LøpendeMåned(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val utbetalingsdato: LocalDate,
) : Periode<LocalDate> {
    /**
     * backing property for vedtaksperioder.
     * Inneholder de vedtaksperioder som er innvilget innenfor en UtbetalingPeriode
     * Implementert som private backing property for å ikke kunne legge til perioder direkte til listen uten å validere den
     */
    private val _vedtaksperioder: MutableList<VedtaksperiodeInnenforLøpendeMåned> = mutableListOf()

    val vedtaksperioder: List<VedtaksperiodeInnenforLøpendeMåned> get() = _vedtaksperioder

    init {
        validatePeriode()
        _vedtaksperioder.forEach { this.inneholder(it) }
    }

    fun medVedtaksperiode(vedtaksperiode: VedtaksperiodeInnenforLøpendeMåned): LøpendeMåned {
        require(inneholder(vedtaksperiode)) {
            "Vedtaksperiode(${vedtaksperiode.formatertPeriodeNorskFormat()}) kan ikke gå utenfor utbetalingsperiode(${this.formatertPeriodeNorskFormat()})"
        }
        _vedtaksperioder.add(vedtaksperiode)
        return this
    }

    fun harDatoerIUkedager(): Boolean = vedtaksperioder.any { it.alleDatoer().any { !it.lørdagEllerSøndag() } }
}

data class MålgruppeOgAktivitet(
    val målgruppe: MålgruppeType,
    val aktivitet: AktivitetBoutgifterBeregningGrunnlag,
) : Comparable<MålgruppeOgAktivitet> {
    override fun compareTo(other: MålgruppeOgAktivitet): Int = COMPARE_BY.compare(this, other)

    companion object {
        //        val COMPARE_BY =
//            compareBy<MålgruppeOgAktivitet> { it.aktivitet.studienivå.prioritet }
//                .thenByDescending { it.aktivitet.prosent }
//                .thenBy { it.målgruppe.prioritet() }
        val COMPARE_BY = compareBy<MålgruppeOgAktivitet>(TODO("Ikke implementert for boutgifter"))
    }
}
