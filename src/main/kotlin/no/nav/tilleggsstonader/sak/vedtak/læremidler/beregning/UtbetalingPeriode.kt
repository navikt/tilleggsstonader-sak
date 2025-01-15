package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate

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

data class GrunnlagForUtbetalingPeriode(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val utbetalingsdato: LocalDate,
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }

    fun tilUtbetalingPeriode(
        stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>,
        aktiviteter: List<AktivitetLæremidlerBeregningGrunnlag>,
    ): UtbetalingPeriode {
        val stønadsperiode = finnRelevantStønadsperiode(stønadsperioder)
        val aktivitet = finnRelevantAktivitet(aktiviteter, stønadsperiode.aktivitet)
        return UtbetalingPeriode(
            fom = fom,
            tom = tom,
            målgruppe = stønadsperiode.målgruppe,
            aktivitet = aktivitet.type,
            studienivå = aktivitet.studienivå,
            prosent = aktivitet.prosent,
            utbetalingsdato = utbetalingsdato,
        )
    }

    private fun finnRelevantAktivitet(
        aktiviteter: List<AktivitetLæremidlerBeregningGrunnlag>,
        aktivitetType: AktivitetType,
    ): AktivitetLæremidlerBeregningGrunnlag {
        val relevanteAktiviteter = aktiviteter.filter { it.type == aktivitetType && it.inneholder(this) }

        brukerfeilHvis(relevanteAktiviteter.isEmpty()) {
            "Det finnes ingen aktiviteter av type $aktivitetType som varer i hele perioden ${this.formatertPeriodeNorskFormat()}}"
        }

        brukerfeilHvis(relevanteAktiviteter.size > 1) {
            "Det finnes mer enn 1 aktivitet i perioden ${this.formatertPeriodeNorskFormat()}. Dette støttes ikke enda. Ta kontakt med TS-sak teamet."
        }

        return relevanteAktiviteter.single()
    }

    private fun finnRelevantStønadsperiode(stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>): StønadsperiodeBeregningsgrunnlag {
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
        val aktivitet: AktivitetLæremidlerBeregningGrunnlag,
    )
}
