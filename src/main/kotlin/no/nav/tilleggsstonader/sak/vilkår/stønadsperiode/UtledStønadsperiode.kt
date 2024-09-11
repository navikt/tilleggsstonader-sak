package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import java.time.LocalDate
import java.util.UUID

/**
 * Denne løsningen finner kombinasjonen av målgruppe og aktivitet
 * Den har ikke noe forhold til ev enhet som brukeren har en viss tidspunkt, som vil gjelde for tiltaksperioder
 */
object UtledStønadsperiode {

    fun utled(behandlingId: UUID, vilkårperioder: List<Vilkårperiode>): List<Stønadsperiode> {
        val perioder = vilkårperioder.filter { it.resultat == ResultatVilkårperiode.OPPFYLT }
        val målgrupper = tilPeriodeVilkår<MålgruppeType>(perioder)
        val aktiviteter = tilPeriodeVilkår<AktivitetType>(perioder)

        return snittMålgruppeAktivitet(målgrupper, aktiviteter)
            .tilSammenslåtteStønadsperioder(behandlingId)
    }

    private fun snittMålgruppeAktivitet(
        målgrupper: Map<MålgruppeType, List<VilkårperiodeHolder>>,
        aktiviteter: Map<AktivitetType, List<VilkårperiodeHolder>>,
    ): List<StønadsperiodeHolder> {
        val map = mutableMapOf<LocalDate, Pair<MålgruppeType, AktivitetType>>()

        målgrupper.keys
            .sortert()
            .map { typeMålgruppe ->
                målgrupper.verdier(typeMålgruppe)
                    .flatMap { målgruppe -> aktiviteter.snittMålgruppe(typeMålgruppe, målgruppe) }
                    .forEach { (typeAktivitet, datoer) ->
                        datoer.forEach { dato ->
                            map.putIfAbsent(dato, Pair(typeMålgruppe, typeAktivitet))
                        }
                    }
            }

        return map
            .map { StønadsperiodeHolder(fom = it.key, tom = it.key, typer = it.value) }
            .toList()
    }

    private fun Map<AktivitetType, List<VilkårperiodeHolder>>.snittMålgruppe(
        typeMålgruppe: MålgruppeType,
        målgruppe: VilkårperiodeHolder,
    ): List<Pair<AktivitetType, List<LocalDate>>> {
        return typeMålgruppe.gyldigeAktiviter.sortert().map { typeAktivitet ->
            val snittDatoer = verdier(typeAktivitet).snittDatoer(målgruppe)
            typeAktivitet to snittDatoer
        }
    }

    private fun List<StønadsperiodeHolder>.tilSammenslåtteStønadsperioder(
        behandlingId: UUID,
    ): List<Stønadsperiode> {
        return this
            .groupBy { it.typer }
            .values
            .flatMap { it.mergeSammenhengende() }
            .tilStønadsperioder(behandlingId)
    }

    private fun List<StønadsperiodeHolder>.tilStønadsperioder(
        behandlingId: UUID,
    ): List<Stønadsperiode> {
        return this.map {
            Stønadsperiode(
                behandlingId = behandlingId,
                fom = it.fom,
                tom = it.tom,
                målgruppe = it.typer.first,
                aktivitet = it.typer.second,
            )
        }
    }

    /**
     * @return alle datoer for snittet mellom målgruppe og aktivitet
     */
    private fun List<VilkårperiodeHolder>.snittDatoer(målgruppe: VilkårperiodeHolder): List<LocalDate> =
        mapNotNull { aktivitet -> aktivitet.snitt(målgruppe)?.alleDatoer() }.flatten()

    private fun Set<MålgruppeType>.sortert() = this.sortedBy { type ->
        prioriterteMålgrupper[type] ?: error("Har ikke mapping for $type")
    }

    @JvmName("sortertAktivitet")
    private fun Set<AktivitetType>.sortert(): List<AktivitetType> = sortedBy { type ->
        prioriterteAktiviteter[type] ?: error("Har ikke mapping for $type")
    }

    // https://confluence.adeo.no/pages/viewpage.action?pageId=140668635
    val prioriterteMålgrupper: Map<MålgruppeType, Int> = MålgruppeType.entries.mapNotNull { type ->
        val verdi = when (type) {
            MålgruppeType.AAP -> 1
            MålgruppeType.NEDSATT_ARBEIDSEVNE -> 2
            MålgruppeType.OVERGANGSSTØNAD -> 3
            MålgruppeType.OMSTILLINGSSTØNAD -> 4
            else -> null
        }
        verdi?.let { type to it }
    }.toMap()

    val prioriterteAktiviteter: Map<AktivitetType, Int> = AktivitetType.entries.mapNotNull { type ->
        val verdi = when (type) {
            AktivitetType.TILTAK -> 1
            AktivitetType.UTDANNING -> 2
            AktivitetType.REELL_ARBEIDSSØKER -> 3
            else -> null
        }
        verdi?.let { type to it }
    }.toMap()

    private inline fun <reified T : VilkårperiodeType> tilPeriodeVilkår(perioder: List<Vilkårperiode>): Map<T, List<VilkårperiodeHolder>> =
        perioder.mapNotNull { periode ->
            if (periode.type is T) {
                periode.type to VilkårperiodeHolder(periode.fom, periode.tom)
            } else {
                null
            }
        }.groupBy({ it.first }, { it.second })

    private fun <T> Map<T, List<VilkårperiodeHolder>>.verdier(type: T) = this.getOrDefault(type, emptyList())

    private data class VilkårperiodeHolder(
        override val fom: LocalDate,
        override val tom: LocalDate,
    ) : Periode<LocalDate> {

        fun snitt(other: VilkårperiodeHolder): VilkårperiodeHolder? {
            return if (this.overlapper(other)) {
                VilkårperiodeHolder(maxOf(this.fom, other.fom), minOf(this.tom, other.tom))
            } else {
                null
            }
        }
    }

    private data class StønadsperiodeHolder(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val typer: Pair<MålgruppeType, AktivitetType>,
    ) : Periode<LocalDate>, Mergeable<LocalDate, StønadsperiodeHolder> {
        override fun merge(other: StønadsperiodeHolder): StønadsperiodeHolder {
            require(typer == other.typer) { "Kan ikke merge 2 ulike" }
            return this.copy(fom = minOf(other.fom, fom), tom = maxOf(other.tom, tom))
        }
    }

    private fun List<StønadsperiodeHolder>.mergeSammenhengende() =
        this.sortedBy { it.fom }.mergeSammenhengende { a, b -> a.tom.plusDays(1) == b.tom }
}
