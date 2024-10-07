package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.automatisk

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.automatisk.StønadsperiodeMapper.tilStønadsperioder
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.automatisk.StønadsperiodeSortering.viktigereEnn
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