package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus

object MålgruppeValidering {
    fun validerKanLeggeTilMålgruppeManuelt(
        stønadstype: Stønadstype,
        målgruppeType: MålgruppeType,
    ) {
        feilHvisIkke(målgruppeType.kanBrukesForStønad(stønadstype)) {
            "målgruppe=$målgruppeType er ikke gyldig for $stønadstype"
        }
    }

    fun validerNyeMålgrupperOverlapperIkkeMedEksisterendeMålgrupper(vilkårperioder: Vilkårperioder) {
        val oppfylteMålgrupper =
            vilkårperioder.målgrupper
                .filter { it.resultat == ResultatVilkårperiode.OPPFYLT }
                .groupBy { it.type }

        val overlappendeNyeMålgrupper =
            oppfylteMålgrupper.values
                .map { målgrupper ->
                    val tidligerePerioder = målgrupper.filter { it.status in setOf(Vilkårstatus.UENDRET, Vilkårstatus.ENDRET) }

                    målgrupper
                        .filter { it.status == Vilkårstatus.NY }
                        .map { nyMålgruppe ->
                            val overlappendePerioder =
                                tidligerePerioder.filter { tidligere -> tidligere.overlapper(nyMålgruppe) }
                            nyMålgruppe to overlappendePerioder
                        }
                }.flatten()
                .filter { it.second.isNotEmpty() }

        brukerfeilHvis(overlappendeNyeMålgrupper.isNotEmpty()) {
            buildString {
                appendLine("Du kan ikke legge til nye målgrupper som overlapper med eksisterende målgrupper:")
                appendLine()
                overlappendeNyeMålgrupper.forEach { (nyMålgruppe, overlappendePerioder) ->
                    appendLine("Perioden ${nyMålgruppe.type} ${nyMålgruppe.formatertPeriodeNorskFormat()} overlapper med:")
                    overlappendePerioder.forEach { tidligereMålgruppe ->
                        appendLine("  - ${tidligereMålgruppe.formatertPeriodeNorskFormat()}")
                    }
                }
            }
        }
    }
}
