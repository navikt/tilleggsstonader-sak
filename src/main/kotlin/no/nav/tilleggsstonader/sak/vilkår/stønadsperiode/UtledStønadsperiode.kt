package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeSortering.viktigereEnn
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

    fun utled(behandlingId: BehandlingId, vilkårperioder: List<Vilkårperiode>, medAntallAktivitetsdager: Boolean): List<Stønadsperiode> {
        return vilkårperioder
            .snittMålgruppeAktivitet(medAntallAktivitetsdager)
            .tilStønadsperioder(behandlingId)
    }

    private fun List<Vilkårperiode>.snittMålgruppeAktivitet(medAntallAktivitetsdager: Boolean): Map<LocalDate, MålgruppeAktivitet> {
        val perioder = this.filter { it.resultat == ResultatVilkårperiode.OPPFYLT }
        val målgrupper = tilPeriodeVilkår<MålgruppeType>(perioder)
        val tilAktiviteter = perioder.tilAktiviteter(medAntallAktivitetsdager)
        val aktiviteter = tilAktiviteter.slåSammenType()

        return mutableMapOf<LocalDate, Pair<MålgruppeAktivitet, Int>>().apply {
            val map = this
            val finnDatoerForSnitt = finnDatoerForSnitt(målgrupper, aktiviteter)
            finnDatoerForSnitt
                .forEach { (typer, datoer) ->
                    datoer.forEach { dato ->
                        val key = dato.first
                        val prevValue = map[key]
                        val pair = Pair(typer, dato.second)
                        if (pair.viktigereEnn(prevValue)) {
                            map[key] = pair
                        }
                    }
                }
        }
            .mapValues { it.value.first }
    }

    /**
     * Itererer over sorterte målgrupper for å finne snitt med sorterte aktiviteter
     */
    private fun finnDatoerForSnitt(
        målgrupper: Map<MålgruppeType, List<VilkårperiodeHolder>>,
        aktiviteter: Map<AktivitetType, List<AktivitetHolder>>,
    ): List<Pair<MålgruppeAktivitet, List<Pair<LocalDate, Int>>>> {
        return målgrupper.keys
            .flatMap { typeMålgruppe ->
                målgrupper
                    .verdier(typeMålgruppe)
                    .flatMap { målgruppe ->
                        typeMålgruppe.gyldigeAktiviter.map { typeAktivitet ->
                            val snittDatoer = aktiviteter.verdier(typeAktivitet).snittDatoer(målgruppe)
                            MålgruppeAktivitet(typeMålgruppe, typeAktivitet) to snittDatoer
                        }
                    }
            }
    }

    /**
     * @return alle datoer for snittet mellom målgruppe og aktivitet
     */
    private fun List<AktivitetHolder>.snittDatoer(målgruppe: VilkårperiodeHolder): List<Pair<LocalDate, Int>> = this
        .mapNotNull { aktivitet ->
            aktivitet.snitt(målgruppe)?.let { it.alleDatoer().map { it to aktivitet.aktivitetsdager } }
        }
        .flatten()

    private inline fun <reified T : VilkårperiodeType> tilPeriodeVilkår(perioder: List<Vilkårperiode>): Map<T, List<VilkårperiodeHolder>> =
        perioder.mapNotNull { periode ->
            if (periode.type is T) {
                periode.type to VilkårperiodeHolder(periode.fom, periode.tom)
            } else {
                null
            }
        }.groupBy({ it.first }, { it.second })

    private fun List<Vilkårperiode>.tilAktiviteter(medAntallAktivitetsdager: Boolean): List<AktivitetHolder> = this
        .mapNotNull { periode ->
            if (periode.type is AktivitetType) {
                val aktivitetsdager = if (medAntallAktivitetsdager) {
                    periode.aktivitetsdager ?: 5
                } else {
                    5
                }
                AktivitetHolder(
                    fom = periode.fom,
                    tom = periode.tom,
                    type = periode.type,
                    aktivitetsdager = aktivitetsdager,
                )
            } else {
                null
            }
        }

    /**
     * Slår sammen aktivitet av samme type og dato, og setter maks antall aktivitetsdager for gitt dato
     */
    private fun List<AktivitetHolder>.slåSammenType(): Map<AktivitetType, List<AktivitetHolder>> {
        val map = mutableMapOf<Pair<AktivitetType, LocalDate>, Int>()
        this.forEach { aktivitet ->
            aktivitet.alleDatoer().forEach {
                val key = Pair(aktivitet.type, it)
                val antallAktivitetsdagerForDato = map.getOrDefault(key, 0)
                map[key] = minOf(5, aktivitet.aktivitetsdager + antallAktivitetsdagerForDato)
            }
        }
        return map.entries
            .map { (key, aktivitetsdager) ->
                AktivitetHolder(fom = key.second, tom = key.second, type = key.first, aktivitetsdager = aktivitetsdager)
            }
            .groupBy { it.type }
            .mapValues {
                it.value
                    .sorted()
                    .mergeSammenhengende { first, second ->
                        val overlapperEllerSammenhengende =
                            first.overlapper(second) || (first.tom.plusDays(1) == second.fom)
                        overlapperEllerSammenhengende && first.aktivitetsdager == second.aktivitetsdager
                    }
            }
    }

    private fun <T, R> Map<T, List<R>>.verdier(type: T): List<R> = this.getOrDefault(type, emptyList())

    private data class VilkårperiodeHolder(
        override val fom: LocalDate,
        override val tom: LocalDate,
    ) : Periode<LocalDate>

    private data class AktivitetHolder(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val type: AktivitetType,
        val aktivitetsdager: Int,
    ) : Periode<LocalDate>, Mergeable<LocalDate, AktivitetHolder> {
        override fun merge(other: AktivitetHolder): AktivitetHolder {
            return this.copy(fom = minOf(this.fom, other.fom), tom = maxOf(this.tom, other.tom))
        }

        fun snitt(other: VilkårperiodeHolder): AktivitetHolder? {
            return if (this.overlapper(other)) {
                this.copy(fom = maxOf(this.fom, other.fom), tom = minOf(this.tom, other.tom))
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

private object StønadsperiodeSortering {

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
