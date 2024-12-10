package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate
import java.time.YearMonth

data class UtbetalingsPeriode(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val utbetalingsMåned: YearMonth,
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
            "Det finnes ingen aktiviteter av type $aktivitetType som varer i hele perioden ${this.fom.norskFormat()} - ${this.tom.norskFormat()}"
        }

        brukerfeilHvis(relevanteAktiviteter.size > 1) {
            "Det finnes mer enn 1 aktivitet i perioden ${this.fom.norskFormat()} - ${this.tom.norskFormat()}. Dette støttes ikke enda. Ta kontakt med TS-sak teamet."
        }

        return relevanteAktiviteter.single()
    }

    fun finnRelevantStønadsperiode(stønadsperioder: List<Stønadsperiode>): Stønadsperiode {
        val relevanteStønadsperioderForPeriode = stønadsperioder
            .mergeSammenhengende(
                skalMerges = { a, b -> a.tom.plusDays(1) == b.fom && a.målgruppe == b.målgruppe && a.aktivitet == b.aktivitet },
                merge = { a, b -> a.copy(tom = b.tom) },
            )
            .filter { it.inneholder(this) }

        feilHvis(relevanteStønadsperioderForPeriode.isEmpty()) {
            "Det finnes ingen periode med overlapp mellom målgruppe og aktivitet for perioden ${this.fom.norskFormat()} - ${this.tom.norskFormat()}"
        }

        feilHvis(relevanteStønadsperioderForPeriode.size > 1) {
            "Det er for mange stønadsperioder som inneholder utbetalingsperioden ${this.fom.norskFormat()} - ${this.tom.norskFormat()}"
        }

        return relevanteStønadsperioderForPeriode.single()
    }
}
