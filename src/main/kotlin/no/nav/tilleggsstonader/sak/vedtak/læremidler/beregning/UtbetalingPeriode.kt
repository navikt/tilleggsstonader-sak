package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate
import java.time.YearMonth

data class UtbetalingPeriode(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val utbetalingsmåned: YearMonth,
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }

    fun finnRelevantAktivitet(
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

    fun finnRelevantStønadsperiode(stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>): StønadsperiodeBeregningsgrunnlag {
        val relevanteStønadsperioderForPeriode = stønadsperioder.filter { it.inneholder(this) }

        feilHvis(relevanteStønadsperioderForPeriode.isEmpty()) {
            "Det finnes ingen periode med overlapp mellom målgruppe og aktivitet for perioden ${this.formatertPeriodeNorskFormat()}"
        }

        feilHvis(relevanteStønadsperioderForPeriode.size > 1) {
            "Det er for mange stønadsperioder som inneholder utbetalingsperioden ${this.formatertPeriodeNorskFormat()}"
        }

        return relevanteStønadsperioderForPeriode.single()
    }
}
