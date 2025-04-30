package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.erFørsteDagIMåneden
import no.nav.tilleggsstonader.sak.util.erSisteDagIMåneden
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.Temporal

sealed class UtgiftBeregningType<T>(
    override val fom: T,
    override val tom: T,
    open val utgift: Int,
) : Periode<T> where T : Comparable<T>, T : Temporal {
    abstract fun tilDatoPeriode(): Datoperiode
}

data class UtgiftBeregningMåned(
    override val fom: YearMonth,
    override val tom: YearMonth,
    override val utgift: Int,
) : UtgiftBeregningType<YearMonth>(fom, tom, utgift) {
    init {
        validatePeriode()
    }

    override fun tilDatoPeriode(): Datoperiode = Datoperiode(fom = fom.atDay(1), tom = tom.atEndOfMonth())
}

data class UtgiftBeregningDato(
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val utgift: Int,
) : UtgiftBeregningType<LocalDate>(fom, tom, utgift) {
    init {
        validatePeriode()
    }

    override fun tilDatoPeriode(): Datoperiode = Datoperiode(fom = fom, tom = tom)
}

fun Vilkår.tilUtgiftBeregning(): UtgiftBeregningType<*> {
    feilHvis(fom == null || tom == null || utgift == null) {
        "Forventer at fra-dato, til-dato og utgift er satt. Gå tilbake til Vilkår-fanen, og legg til datoer og utgifter der. For utviklerteamet: dette gjelder vilkår=$id."
    }
    feilHvis(type.skalHaDatoerFørsteOgSisteIMåneden() && !fom.erFørsteDagIMåneden()) {
        "Noe er feil. Fom skal være satt til første dagen i måneden"
    }
    feilHvis(type.skalHaDatoerFørsteOgSisteIMåneden() && !tom.erSisteDagIMåneden()) {
        "Noe er feil. Tom skal være satt til siste dagen i måneden"
    }
    return when (type) {
        VilkårType.LØPENDE_UTGIFTER_EN_BOLIG, VilkårType.LØPENDE_UTGIFTER_TO_BOLIGER, VilkårType.PASS_BARN ->
            UtgiftBeregningMåned(
                fom = YearMonth.from(fom),
                tom = YearMonth.from(tom),
                utgift = utgift,
            )

        VilkårType.UTGIFTER_OVERNATTING ->
            UtgiftBeregningDato(
                fom = fom,
                tom = tom,
                utgift = utgift,
            )
    }
}

private fun VilkårType.skalHaDatoerFørsteOgSisteIMåneden(): Boolean = this.erLøpendeBoutgifter() || this == VilkårType.PASS_BARN
