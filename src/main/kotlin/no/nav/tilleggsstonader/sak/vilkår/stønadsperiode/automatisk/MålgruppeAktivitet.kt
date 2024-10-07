package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.automatisk

import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.automatisk.PrioriteringStønadsperioder.prioritet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

data class MålgruppeAktivitet(
    val målgruppe: MålgruppeType,
    val aktivitet: AktivitetType,
    val aktivitetsdager: Int,
) : Comparable<MålgruppeAktivitet> {

    override fun compareTo(other: MålgruppeAktivitet): Int {
        val aktivitetsdagerCompareTo = this.aktivitetsdager.compareTo(other.aktivitetsdager)
        if (aktivitetsdagerCompareTo != 0) {
            return aktivitetsdagerCompareTo
        }

        val målgruppeCompareTo = this.målgruppe.prioritet().compareTo(other.målgruppe.prioritet())
        if (målgruppeCompareTo != 0) {
            return målgruppeCompareTo
        }

        val aktivitetCompareTo = this.aktivitet.prioritet().compareTo(other.aktivitet.prioritet())
        if (aktivitetCompareTo != 0) {
            return aktivitetCompareTo
        }
        // like
        return 0
    }
}
