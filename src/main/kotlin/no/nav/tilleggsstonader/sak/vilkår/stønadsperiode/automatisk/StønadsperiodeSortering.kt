package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.automatisk

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

object StønadsperiodeSortering {

    fun Pair<MålgruppeAktivitet, Int>.viktigereEnn(other: Pair<MålgruppeAktivitet, Int>?): Boolean {
        if (other == null) {
            return true
        }
        val aktivitetsdagerCompareTo = this.second.compareTo(other.second)
        when {
            aktivitetsdagerCompareTo > 0 -> return true
            aktivitetsdagerCompareTo < 0 -> return false
        }

        val målgruppeCompareTo = this.first.målgruppe.prioritet().compareTo(other.first.målgruppe.prioritet())
        when {
            målgruppeCompareTo > 0 -> return true
            målgruppeCompareTo < 0 -> return false
        }

        val aktivitetCompareTo = this.first.aktivitet.prioritet().compareTo(other.first.aktivitet.prioritet())
        when {
            aktivitetCompareTo > 0 -> return true
            aktivitetCompareTo < 0 -> return false
        }
        // like
        error("Burde ikke finnes 2 like")
    }

    private fun MålgruppeType.prioritet(): Int =
        prioriterteMålgrupper[this] ?: error("Har ikke mapping for $this")

    @JvmName("prioritetAktivitet")
    private fun AktivitetType.prioritet(): Int =
        prioriterteAktiviteter[this] ?: error("Har ikke mapping for $this")

    // https://confluence.adeo.no/pages/viewpage.action?pageId=140668635
    val prioriterteMålgrupper: Map<MålgruppeType, Int> = MålgruppeType.entries.mapNotNull { type ->
        val verdi = when (type) {
            MålgruppeType.AAP -> 4
            MålgruppeType.NEDSATT_ARBEIDSEVNE -> 3
            MålgruppeType.OVERGANGSSTØNAD -> 2
            MålgruppeType.OMSTILLINGSSTØNAD -> 1
            else -> null
        }
        verdi?.let { type to it }
    }.toMap()

    val prioriterteAktiviteter: Map<AktivitetType, Int> = AktivitetType.entries.mapNotNull { type ->
        val verdi = when (type) {
            AktivitetType.TILTAK -> 3
            AktivitetType.UTDANNING -> 2
            AktivitetType.REELL_ARBEIDSSØKER -> 1
            else -> null
        }
        verdi?.let { type to it }
    }.toMap()
}