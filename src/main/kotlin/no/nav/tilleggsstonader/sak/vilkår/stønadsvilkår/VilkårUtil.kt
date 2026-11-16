package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDagligReisePrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
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
            vilkår.type.erLøpendeOgSkalSlåsSammen() &&
                vilkår.type == other.vilkår.type &&
                vilkår.resultat == other.vilkår.resultat &&
                vilkår.utgift == other.vilkår.utgift &&
                harLikeFakta(other) &&
                vilkår.barnId == other.vilkår.barnId &&
                overlapperEllerPåfølgesAv(other)

        private fun harLikeFakta(other: VilkårHolder): Boolean {
            val fakta = vilkår.fakta
            val otherFakta = other.vilkår.fakta
            return if (fakta is FaktaDagligReisePrivatBil && otherFakta is FaktaDagligReisePrivatBil) {
                fakta.totalReiseavstand.compareTo(otherFakta.totalReiseavstand) == 0 &&
                    fakta.copy(totalReiseavstand = otherFakta.totalReiseavstand) == otherFakta
            } else {
                fakta == otherFakta
            }
        }

        private fun VilkårType.erLøpendeOgSkalSlåsSammen() =
            when (this) {
                VilkårType.PASS_BARN -> true
                VilkårType.UTGIFTER_OVERNATTING -> false
                VilkårType.LØPENDE_UTGIFTER_EN_BOLIG -> true
                VilkårType.LØPENDE_UTGIFTER_TO_BOLIGER -> true
                VilkårType.DAGLIG_REISE -> true
                VilkårType.REISE_TIL_SAMLING -> false // TODO: Sjekk om dette er riktig
                VilkårType.ROAH_OPPSTART_AVSLUTNING -> false
                VilkårType.ROAH_HJEMREISE -> false
                VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE -> false
                VilkårType.FLYTTING -> false
                VilkårType.EKSEMPEL2 -> false
                VilkårType.EKSEMPEL -> false
            }
    }
}
