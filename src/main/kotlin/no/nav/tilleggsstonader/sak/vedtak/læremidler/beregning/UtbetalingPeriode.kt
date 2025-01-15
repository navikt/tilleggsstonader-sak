package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate

/**
 * Utbetalingperiode for løpende måned
 * Eks 5jan - 4feb
 *
 * @param utbetalingsdato utbetalingsdato for når en utbetalingsperiode skal utbetales.
 * Eks hvis man innvilger jan-juni så skal man utbetale hele beløpet for fom i første utbetalingsperioden,
 * dvs 5 jan i tidligere eksemplet
 *
 * @param vedtaksperioder inneholder de vedtaksperioder som er innvilget innenfor en UtbetalingPeriode
 * Implementert som private backing property for å ikke kunne legge til perioder direkt til listen uten å validere den
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
    }
}

class GrunnlagForUtbetalingPeriode private constructor(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val utbetalingsdato: LocalDate,
    // TODO vedtaksperiode med målgruppe og aktivitet
    private val _vedtaksperioder: MutableList<Vedtaksperiode>,
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }

    constructor(fom: LocalDate, tom: LocalDate, utbetalingsdato: LocalDate) : this(
        fom = fom,
        tom = tom,
        utbetalingsdato = utbetalingsdato,
        mutableListOf()
    )

    fun medVedtaksperiode(vedtaksperiode: Vedtaksperiode): GrunnlagForUtbetalingPeriode {
        require(inneholder(vedtaksperiode)) {
            "Vedtaksperiode(${vedtaksperiode.formatertPeriodeNorskFormat()}) kan ikke gå utenfor utbetalingsperiode(${this.formatertPeriodeNorskFormat()})"
        }
        _vedtaksperioder.add(vedtaksperiode)
        return this
    }

    fun finnMålgruppeOgAktivitet(
        stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>,
        aktiviteter: List<Aktivitet>,
    ): UtbetalingPeriode {
        val målgruppeOgAktiviteter = this._vedtaksperioder
            .map { vedtaksperiode ->
                val stønadsperiode = vedtaksperiode.finnRelevantStønadsperiode(stønadsperioder)
                val aktivitet = vedtaksperiode.finnRelevantAktivitet(aktiviteter, stønadsperiode.aktivitet)
                MålgruppeOgAktivitet(stønadsperiode.målgruppe, aktivitet)
            }

        /**
         * TODO finn målgruppe/aktivitet som gir mest rettighet
         */
        val målgruppe = målgruppeOgAktiviteter.first().målgruppe
        require(målgruppeOgAktiviteter.all { it.målgruppe == målgruppe }) {
            "Alle målgrupper innenfor en utbetalingsperiode må være av samme typen"
        }
        val aktivitet = målgruppeOgAktiviteter.map { it.aktivitet }.distinct().single()
        return UtbetalingPeriode(
            fom = fom,
            tom = tom,
            målgruppe = målgruppe,
            aktivitet = aktivitet.type,
            studienivå = aktivitet.studienivå,
            prosent = aktivitet.prosent,
            utbetalingsdato = utbetalingsdato
        )
    }

    private fun Vedtaksperiode.finnRelevantAktivitet(
        aktiviteter: List<Aktivitet>,
        aktivitetType: AktivitetType,
    ): Aktivitet {
        val relevanteAktiviteter = aktiviteter.filter { it.type == aktivitetType && it.inneholder(this) }

        brukerfeilHvis(relevanteAktiviteter.isEmpty()) {
            "Det finnes ingen aktiviteter av type $aktivitetType som varer i hele perioden ${this.formatertPeriodeNorskFormat()}}"
        }

        brukerfeilHvis(relevanteAktiviteter.size > 1) {
            "Det finnes mer enn 1 aktivitet i perioden ${this.formatertPeriodeNorskFormat()}. Dette støttes ikke enda. Ta kontakt med TS-sak teamet."
        }

        return relevanteAktiviteter.single()
    }

    private fun Vedtaksperiode.finnRelevantStønadsperiode(stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>): StønadsperiodeBeregningsgrunnlag {
        val relevanteStønadsperioderForPeriode = stønadsperioder.filter { it.inneholder(this) }

        feilHvis(relevanteStønadsperioderForPeriode.isEmpty()) {
            "Det finnes ingen periode med overlapp mellom målgruppe og aktivitet for perioden ${this.formatertPeriodeNorskFormat()}"
        }

        feilHvis(relevanteStønadsperioderForPeriode.size > 1) {
            "Det er for mange stønadsperioder som inneholder utbetalingsperioden ${this.formatertPeriodeNorskFormat()}"
        }

        return relevanteStønadsperioderForPeriode.single()
    }

    private class MålgruppeOgAktivitet(
        val målgruppe: MålgruppeType,
        val aktivitet: Aktivitet,
    )

}
