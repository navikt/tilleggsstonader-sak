package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import java.time.LocalDate

object VilkårUtil {
    fun List<Vilkår>.slåSammenSammenhengende(): List<Vilkår> {
        val (perioderMedTom, perioderUtenTom) = this.partition { it.tom != null }

        val sammenslåttePerioder =
            perioderMedTom
                .map { VilkårHolder(it) }
                .sortedWith(compareBy({ it.vilkår.type }, { it }))
                .mergeSammenhengende(
                    skalMerges = { v1, v2 -> v1.kanSlåsSammen(v2) },
                    merge = { v1, v2 -> v1.slåSammen(v2) },
                ).map { it.vilkår }

        return (sammenslåttePerioder + perioderUtenTom).sortedBy { it.fom }
    }

    /**
     * Hjelpeklasse for å kunne bruke funksjonene som finnes på Periode<T>
     */
    private data class VilkårHolder(
        val vilkår: Vilkår,
    ) : Periode<LocalDate>,
        KopierPeriode<VilkårHolder> {
        override val fom: LocalDate
            get() = vilkår.fom ?: error("Mangler tom")
        override val tom: LocalDate
            get() = vilkår.tom ?: error("Mangler tom")

        override fun medPeriode(
            fom: LocalDate,
            tom: LocalDate,
        ): VilkårHolder = VilkårHolder(vilkår.copy(fom = fom, tom = tom))

        fun slåSammen(other: VilkårHolder): VilkårHolder =
            VilkårHolder(
                vilkår.copy(
                    fom = minOf(vilkår.fom!!, other.vilkår.fom!!),
                    tom = maxOf(vilkår.tom!!, other.vilkår.tom!!),
                ),
            )

        fun kanSlåsSammen(other: VilkårHolder): Boolean =
            vilkår.type == other.vilkår.type &&
                vilkår.resultat == other.vilkår.resultat &&
                vilkår.utgift == other.vilkår.utgift &&
                vilkår.barnId == other.vilkår.barnId &&
                overlapperEllerPåfølgesAv(other)
    }
}
