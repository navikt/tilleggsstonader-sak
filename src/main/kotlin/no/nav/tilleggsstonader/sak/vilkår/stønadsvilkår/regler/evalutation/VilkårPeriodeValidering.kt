package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.førsteOverlappendePeriode
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType

object VilkårPeriodeValidering {
    /**
     * Validerer at vilkår for av samme type/barn ikke overlapper
     * 2 ulike barn med samme periode kan overlappe
     * 1 enkelt barn kan ikke ha overlappende perioder
     * Vilkår av gitt vilkårtype uten tilknytting til barn kan ikke overlappe
     */
    fun validerIkkeOverlappendeVilkår(values: List<Vilkår>) {
        values
            .filterNot { it.status == VilkårStatus.SLETTET }
            .groupBy { Pair(it.type, it.barnId) }
            .mapValues { (_, vilkårliste) -> vilkårTilDatoperiode(vilkårliste) }
            .forEach { (vilkårType, vilkårliste) -> validerIkkeOverlappende(vilkårType.first, vilkårliste) }
    }

    private fun validerIkkeOverlappende(
        vilkårType: VilkårType,
        vilkårliste: List<Datoperiode>,
    ) {
        val overlappendePeriode = vilkårliste.førsteOverlappendePeriode()
        // Hopp over validering da det må være lov, dersom man f.eks først skal reise med tog så buss.
        if (vilkårType.gjelderStønader.contains(Stønadstype.DAGLIG_REISE_TSO) ||
            vilkårType.gjelderStønader.contains(Stønadstype.DAGLIG_REISE_TSR)
        ) {
            return
        }
        if (overlappendePeriode != null) {
            brukerfeil(
                "Det er ikke gyldig med overlappende perioder for ${vilkårType.tilFeilmeldingTekst()}. " +
                    "Periode ${overlappendePeriode.first.formatertPeriodeNorskFormat()} " +
                    "overlapper med ${overlappendePeriode.second.formatertPeriodeNorskFormat()}. ",
            )
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

    private fun VilkårType.tilFeilmeldingTekst(): String =
        when (this) {
            VilkårType.PASS_BARN -> "et barn"
            else -> beskrivelse.lowercase()
        }
}
