package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.UtledStønadsperiodeMapper.tilStønadsperioder
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import java.time.LocalDate

/**
 * Denne løsningen finner kombinasjonen av målgruppe og aktivitet
 * Den har ikke noe forhold til ev enhet som brukeren har en viss tidspunkt, som vil gjelde for tiltaksperioder
 *
 * Den finner "prioriterte" målgruppe/aktivitet-kombinasjoner, sånn at AAP trumfer Overgangsstønad
 */
object UtledStønadsperiode {

    fun utled(behandlingId: BehandlingId, vilkårperioder: List<Vilkårperiode>): List<Stønadsperiode> {
        return vilkårperioder
            .snittMålgruppeAktivitet()
            .tilStønadsperioder(behandlingId)
    }

    private fun List<Vilkårperiode>.snittMålgruppeAktivitet(): Map<LocalDate, MålgruppeAktivitet> {
        val perioder = this.filter { it.resultat == ResultatVilkårperiode.OPPFYLT }
        val målgrupper = tilPeriodeVilkår<MålgruppeType>(perioder)
        val aktiviteter = tilPeriodeVilkår<AktivitetType>(perioder)

        return mutableMapOf<LocalDate, MålgruppeAktivitet>().apply {
            finnDatoerForSnitt(målgrupper, aktiviteter)
                .forEach { (typer, datoer) ->
                    datoer.forEach { dato -> putIfAbsent(dato, typer) }
                }
        }
    }

    /**
     * Itererer over sorterte målgrupper for å finne snitt med sorterte aktiviteter
     */
    private fun finnDatoerForSnitt(
        målgrupper: Map<MålgruppeType, List<VilkårperiodeHolder>>,
        aktiviteter: Map<AktivitetType, List<VilkårperiodeHolder>>,
    ): List<Pair<MålgruppeAktivitet, List<LocalDate>>> {
        return målgrupper.keys
            .sortert()
            .flatMap { typeMålgruppe ->
                val gyldigeAktiviteter = typeMålgruppe.gyldigeAktiviter.sortert()
                målgrupper
                    .verdier(typeMålgruppe)
                    .flatMap { målgruppe ->
                        gyldigeAktiviteter.map { typeAktivitet ->
                            val snittDatoer = aktiviteter.verdier(typeAktivitet).snittDatoer(målgruppe)
                            MålgruppeAktivitet(typeMålgruppe, typeAktivitet) to snittDatoer
                        }
                    }
            }
    }

    /**
     * @return alle datoer for snittet mellom målgruppe og aktivitet
     */
    private fun List<VilkårperiodeHolder>.snittDatoer(målgruppe: VilkårperiodeHolder): List<LocalDate> = this
        .mapNotNull { aktivitet -> aktivitet.snitt(målgruppe) }
        .map { it.alleDatoer() }
        .flatten()

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


}

private data class MålgruppeAktivitet(
    val målgruppe: MålgruppeType,
    val aktivitet: AktivitetType,
)

/**
 * Util for å mappe, slå sammen og mappe til stønadsperioder
 */
private object UtledStønadsperiodeMapper {

    fun Map<LocalDate, MålgruppeAktivitet>.tilStønadsperioder(behandlingId: BehandlingId) = this
        .map { StønadsperiodeHolder(fom = it.key, tom = it.key, målgruppeAktivitet = it.value) }
        .slåSammen()
        .tilStønadsperiodeHolder(behandlingId)

    /**
     * Grupperer perioder på målgruppe og aktivitet
     * Merger sammenhengende tvers grupperingen
     */
    private fun List<StønadsperiodeHolder>.slåSammen(): List<StønadsperiodeHolder> {
        return this
            .groupBy { it.målgruppeAktivitet }
            .values
            .flatMap { perioderPerGruppe ->
                perioderPerGruppe
                    .sorted()
                    .mergeSammenhengende { a, b ->
                        a.tom.plusDays(1) == b.tom
                    }
            }
    }

    private fun List<StønadsperiodeHolder>.tilStønadsperiodeHolder(behandlingId: BehandlingId): List<Stønadsperiode> {
        return this.map {
            Stønadsperiode(
                behandlingId = behandlingId,
                fom = it.fom,
                tom = it.tom,
                målgruppe = it.målgruppeAktivitet.målgruppe,
                aktivitet = it.målgruppeAktivitet.aktivitet,
            )
        }
    }

    private data class StønadsperiodeHolder(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val målgruppeAktivitet: MålgruppeAktivitet,
    ) : Periode<LocalDate>, Mergeable<LocalDate, StønadsperiodeHolder> {
        override fun merge(other: StønadsperiodeHolder): StønadsperiodeHolder {
            require(målgruppeAktivitet == other.målgruppeAktivitet) { "Kan ikke merge 2 ulike" }
            return this.copy(fom = minOf(other.fom, fom), tom = maxOf(other.tom, tom))
        }
    }
}
