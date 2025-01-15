package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerSplitPerLøpendeMånedUtil.sisteDagenILøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
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
        grunnlagForUtbetalingPeriode: GrunnlagForUtbetalingPeriode,
        stønadsperiode: StønadsperiodeBeregningsgrunnlag,
        aktivitet: AktivitetLæremidlerBeregningGrunnlag,
    ) : this(
        fom = grunnlagForUtbetalingPeriode.fom,
        tom = grunnlagForUtbetalingPeriode.vedtaksperioder.maxOf { it.tom },
        målgruppe = stønadsperiode.målgruppe,
        aktivitet = stønadsperiode.aktivitet,
        studienivå = aktivitet.studienivå,
        prosent = aktivitet.prosent,
        utbetalingsdato = grunnlagForUtbetalingPeriode.utbetalingsdato,
    )
}

data class GrunnlagForUtbetalingPeriode(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val utbetalingsdato: LocalDate,
) : Periode<LocalDate> {

    /**
     * backing property for vedtaksperioder.
     * Inneholder de vedtaksperioder som er innvilget innenfor en UtbetalingPeriode
     * Implementert som private backing property for å ikke kunne legge til perioder direkte til listen uten å validere den
     */
    private val _vedtaksperioder: MutableList<Vedtaksperiode> = mutableListOf()

    val vedtaksperioder: List<Vedtaksperiode> get() = _vedtaksperioder

    init {
        validatePeriode()
        _vedtaksperioder.forEach { this.inneholder(it) }
    }

    fun medVedtaksperiode(vedtaksperiode: Vedtaksperiode): GrunnlagForUtbetalingPeriode {
        require(inneholder(vedtaksperiode)) {
            "Vedtaksperiode(${vedtaksperiode.formatertPeriodeNorskFormat()}) kan ikke gå utenfor utbetalingsperiode(${this.formatertPeriodeNorskFormat()})"
        }
        _vedtaksperioder.add(vedtaksperiode)
        return this
    }

    fun tilUtbetalingPeriode(
        stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>,
        aktiviteter: List<AktivitetLæremidlerBeregningGrunnlag>,
    ): UtbetalingPeriode {
        val stønadsperiode = finnRelevantStønadsperiode(stønadsperioder)
        val aktivitet = finnRelevantAktivitet(aktiviteter, stønadsperiode.aktivitet)
        return UtbetalingPeriode(this, stønadsperiode, aktivitet)
    }

    private fun finnRelevantAktivitet(
        aktiviteter: List<AktivitetLæremidlerBeregningGrunnlag>,
        aktivitetType: AktivitetType,
    ): AktivitetLæremidlerBeregningGrunnlag {
        val relevanteAktiviteter = aktiviteter
            .filter { it.type == aktivitetType }
            .filter { it.overlapper(this) }

        brukerfeilHvis(relevanteAktiviteter.isEmpty()) {
            "Det finnes ingen aktiviteter av type $aktivitetType som varer i hele perioden ${this.formatertPeriodeNorskFormat()}}"
        }

        brukerfeilHvis(relevanteAktiviteter.size > 1) {
            "Det finnes mer enn 1 aktivitet i perioden ${this.formatertPeriodeNorskFormat()}. Dette støttes ikke enda. Ta kontakt med TS-sak teamet."
        }

        return relevanteAktiviteter.single()
    }

    private fun finnRelevantStønadsperiode(stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>): StønadsperiodeBeregningsgrunnlag {
        val relevanteStønadsperioderForPeriode = stønadsperioder
            .filter { it.overlapper(this) }

        feilHvis(relevanteStønadsperioderForPeriode.isEmpty()) {
            "Det finnes ingen periode med overlapp mellom målgruppe og aktivitet for perioden ${this.formatertPeriodeNorskFormat()}"
        }

        feilHvis(relevanteStønadsperioderForPeriode.size > 1) {
            "Det er for mange stønadsperioder som inneholder utbetalingsperioden ${this.formatertPeriodeNorskFormat()}"
        }

        return relevanteStønadsperioderForPeriode.single()
    }
}
