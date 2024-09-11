package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.førsteOverlappendePeriode
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import java.time.LocalDate

object VilkårPeriodeValidering {

    /**
     * Validerer at vilkår for av samme type/barn ikke overlapper
     * 2 ulike barn med samme periode kan overlappe
     * 1 enkelt barn kan ikke ha overlappende perioder
     * Vilkår av gitt vilkårtype uten tilknytting til barn kan ikke overlappe
     */
    fun validerIkkeOverlappendeVilkår(values: List<Vilkår>) {
        values
            .groupBy { Pair(it.type, it.barnId) }
            .mapValues { (_, vilkårliste) -> vilkårTilDatoperiode(vilkårliste) }
            .forEach { (_, vilkårliste) -> validerIkkeOverlappende(vilkårliste) }
    }

    private fun validerIkkeOverlappende(vilkårliste: List<Datoperiode>) {
        val overlappendePeriode = vilkårliste.førsteOverlappendePeriode()
        if (overlappendePeriode != null) {
            brukerfeil("Periode=${overlappendePeriode.first} og ${overlappendePeriode.second} overlapper.")
        }
    }

    private fun vilkårTilDatoperiode(vilkårliste: List<Vilkår>) =
        vilkårliste.mapNotNull {
            if (it.fom == null || it.tom == null) {
                null
            } else {
                Datoperiode(fom = it.fom, tom = it.tom)
            }
        }

    private data class Datoperiode(
        override val fom: LocalDate,
        override val tom: LocalDate,
    ) : Periode<LocalDate>
}
