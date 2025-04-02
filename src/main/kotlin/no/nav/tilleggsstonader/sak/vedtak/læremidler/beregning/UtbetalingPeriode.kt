package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.kontrakter.periode.beregnSnitt
import no.nav.tilleggsstonader.kontrakter.periode.mergeOverlappende
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.util.lørdagEllerSøndag
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningsgrunnlagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerVedtaksperiodeUtil.sisteDagenILøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
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
    val studienivå: Studienivå,
    val prosent: Int,
    val utbetalingsdato: LocalDate,
) : Periode<LocalDate> {
    init {
        validatePeriode()
        require(tom <= fom.sisteDagenILøpendeMåned()) {
            "UtbetalingPeriode kan ikke løpe lengre enn en løpende måned"
        }
    }

    constructor(
        løpendeMåned: LøpendeMåned,
        målgruppeOgAktivitet: MålgruppeOgAktivitet,
    ) : this(
        fom = løpendeMåned.fom,
        tom = løpendeMåned.vedtaksperioder.maxOf { it.tom },
        målgruppe = målgruppeOgAktivitet.målgruppe,
        aktivitet = målgruppeOgAktivitet.aktivitet.type,
        studienivå = målgruppeOgAktivitet.aktivitet.studienivå,
        prosent = målgruppeOgAktivitet.aktivitet.prosent,
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

    /**
     * Finner hvilken stønadsperiode og aktivitet som skal brukes for den aktuelle utbetalingsperioden
     */
    fun tilUtbetalingPeriode(
        vedtaksperioderBeregningsgrunnlag: List<VedtaksperiodeBeregningsgrunnlagLæremidler>,
        aktiviteter: List<AktivitetLæremidlerBeregningGrunnlag>,
    ): UtbetalingPeriode {
        require(vedtaksperioder.isNotEmpty()) {
            "Kan ikke lage UtbetalingPeriode når vedtaksperioder er tom"
        }
        val sorterteMålgruppeOgAktivitet =
            vedtaksperioder
                .flatMap { vedtaksperiode ->
                    vedtaksperiode.finnRelevantMålgruppeOgAktivitet(vedtaksperioderBeregningsgrunnlag, aktiviteter)
                }.sorted()

        return UtbetalingPeriode(this, sorterteMålgruppeOgAktivitet.first())
    }

    private fun VedtaksperiodeInnenforLøpendeMåned.finnRelevantMålgruppeOgAktivitet(
        vedtaksperioderBeregningsgrunnlag: List<VedtaksperiodeBeregningsgrunnlagLæremidler>,
        aktiviteter: List<AktivitetLæremidlerBeregningGrunnlag>,
    ) = this
        .finnSnittAvRelevanteVedtaksperioder(vedtaksperioderBeregningsgrunnlag)
        .flatMap { stønadsperiode ->
            this
                .finnSnittAvRelevanteAktiviteter(aktiviteter, stønadsperiode)
                .map { aktivitet -> MålgruppeOgAktivitet(stønadsperiode.målgruppe, aktivitet) }
        }

    private fun VedtaksperiodeInnenforLøpendeMåned.finnSnittAvRelevanteAktiviteter(
        aktiviteter: List<AktivitetLæremidlerBeregningGrunnlag>,
        vedtaksperioderBeregningsgrunnlag: VedtaksperiodeBeregningsgrunnlagLæremidler,
    ): List<AktivitetLæremidlerBeregningGrunnlag> {
        val relevanteAktiviteter =
            aktiviteter
                .filter { it.type == vedtaksperioderBeregningsgrunnlag.aktivitet }
                .mapNotNull { it.beregnSnitt(vedtaksperioderBeregningsgrunnlag) }
                .mapNotNull { it.beregnSnitt(this) }
                .mergeOverlappende(
                    erLike = { aktivitet1, aktivitet2 -> aktivitet1.studienivå == aktivitet2.studienivå },
                    merge = { aktivitet1, aktivitet2 ->
                        aktivitet1.copy(prosent = minOf(100, aktivitet1.prosent + aktivitet2.prosent))
                    },
                )

        brukerfeilHvis(relevanteAktiviteter.isEmpty()) {
            "Det finnes ingen aktiviteter av type ${vedtaksperioderBeregningsgrunnlag.aktivitet} som varer i hele perioden ${this.formatertPeriodeNorskFormat()}}"
        }

        feilHvis(relevanteAktiviteter.overlapper()) {
            "Det er foreløpig ikke støtte for flere aktiviteter med ulike studienivåer som overlapper" +
                " (gjelder perioden ${this.formatertPeriodeNorskFormat()}). " +
                "Ta kontakt med utviklerteamet for å forstå situasjonen og om det burde legges til støtte for det."
        }

        return relevanteAktiviteter
    }

    private fun VedtaksperiodeInnenforLøpendeMåned.finnSnittAvRelevanteVedtaksperioder(
        stønadsperioder: List<VedtaksperiodeBeregningsgrunnlagLæremidler>,
    ): List<VedtaksperiodeBeregningsgrunnlagLæremidler> {
        val relevanteVedtaksperForPeriode = stønadsperioder.mapNotNull { it.beregnSnitt(this) }

        feilHvis(relevanteVedtaksperForPeriode.isEmpty()) {
            "Det finnes ingen periode med overlapp mellom målgruppe og aktivitet for perioden ${this.formatertPeriodeNorskFormat()}"
        }

        return relevanteVedtaksperForPeriode
    }
}

data class MålgruppeOgAktivitet(
    val målgruppe: MålgruppeType,
    val aktivitet: AktivitetLæremidlerBeregningGrunnlag,
) : Comparable<MålgruppeOgAktivitet> {
    override fun compareTo(other: MålgruppeOgAktivitet): Int = COMPARE_BY.compare(this, other)

    companion object {
        val COMPARE_BY =
            compareBy<MålgruppeOgAktivitet> { it.aktivitet.studienivå.prioritet }
                .thenByDescending { it.aktivitet.prosent }
                .thenBy { it.målgruppe.prioritet() }
    }
}
