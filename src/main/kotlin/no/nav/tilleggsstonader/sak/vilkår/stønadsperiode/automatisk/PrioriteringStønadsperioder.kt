package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.automatisk

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

object PrioriteringStønadsperioder {

    fun MålgruppeType.prioritet(): Int =
        prioriterteMålgrupper[this] ?: error("Har ikke mapping for $this")

    @JvmName("prioritetAktivitet")
    fun AktivitetType.prioritet(): Int =
        prioriterteAktiviteter[this] ?: error("Har ikke mapping for $this")

    // https://confluence.adeo.no/pages/viewpage.action?pageId=140668635
    private val prioriterteMålgrupper: Map<MålgruppeType, Int> = MålgruppeType.entries.mapNotNull { type ->
        val verdi = when (type) {
            MålgruppeType.AAP -> 4
            MålgruppeType.NEDSATT_ARBEIDSEVNE -> 3
            MålgruppeType.OVERGANGSSTØNAD -> 2
            MålgruppeType.OMSTILLINGSSTØNAD -> 1
            else -> null
        }
        verdi?.let { type to it }
    }.toMap()

    private val prioriterteAktiviteter: Map<AktivitetType, Int> = AktivitetType.entries.mapNotNull { type ->
        val verdi = when (type) {
            AktivitetType.TILTAK -> 3
            AktivitetType.UTDANNING -> 2
            AktivitetType.REELL_ARBEIDSSØKER -> 1
            else -> null
        }
        verdi?.let { type to it }
    }.toMap()
}
