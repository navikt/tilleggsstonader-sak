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
        feilHvisIkke(kanMålgruppeBrukesForStønad(stønadstype, målgruppeType)) {
            "målgruppe=$målgruppeType er ikke gyldig for $stønadstype"
        }
    }

    fun kanMålgruppeBrukesForStønad(
        stønadstype: Stønadstype,
        målgruppeType: MålgruppeType,
    ): Boolean =
        when (stønadstype) {
            Stønadstype.BARNETILSYN, Stønadstype.LÆREMIDLER, Stønadstype.BOUTGIFTER ->
                when (målgruppeType) {
                    MålgruppeType.AAP -> true
                    MålgruppeType.DAGPENGER -> false
                    MålgruppeType.NEDSATT_ARBEIDSEVNE -> true
                    MålgruppeType.OMSTILLINGSSTØNAD -> true
                    MålgruppeType.OVERGANGSSTØNAD -> true
                    MålgruppeType.UFØRETRYGD -> true
                    MålgruppeType.SYKEPENGER_100_PROSENT -> true
                    MålgruppeType.INGEN_MÅLGRUPPE -> true
                    MålgruppeType.TILTAKSPENGER -> false
                    MålgruppeType.KVALIFISERINGSSTØNAD -> false
                }

            Stønadstype.DAGLIG_REISE_TSO ->
                when (målgruppeType) {
                    MålgruppeType.AAP -> true
                    MålgruppeType.DAGPENGER -> false
                    MålgruppeType.NEDSATT_ARBEIDSEVNE -> true
                    MålgruppeType.OMSTILLINGSSTØNAD -> true
                    MålgruppeType.OVERGANGSSTØNAD -> true
                    MålgruppeType.UFØRETRYGD -> true
                    MålgruppeType.SYKEPENGER_100_PROSENT -> false
                    MålgruppeType.INGEN_MÅLGRUPPE -> true
                    MålgruppeType.TILTAKSPENGER -> false
                    MålgruppeType.KVALIFISERINGSSTØNAD -> false
                }

            Stønadstype.DAGLIG_REISE_TSR ->
                when (målgruppeType) {
                    MålgruppeType.INGEN_MÅLGRUPPE -> true
                    MålgruppeType.TILTAKSPENGER -> true
                    MålgruppeType.KVALIFISERINGSSTØNAD -> true
                    MålgruppeType.AAP -> false
                    MålgruppeType.DAGPENGER -> true
                    MålgruppeType.NEDSATT_ARBEIDSEVNE -> false
                    MålgruppeType.OMSTILLINGSSTØNAD -> false
                    MålgruppeType.OVERGANGSSTØNAD -> false
                    MålgruppeType.UFØRETRYGD -> false
                    MålgruppeType.SYKEPENGER_100_PROSENT -> false
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
