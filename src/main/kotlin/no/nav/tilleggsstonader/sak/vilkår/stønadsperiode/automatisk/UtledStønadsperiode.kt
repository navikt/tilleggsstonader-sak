package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.automatisk

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.automatisk.StønadsperiodeMapper.tilStønadsperioder
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import java.time.LocalDate

/**
 * Denne løsningen finner kombinasjonen av målgruppe og aktivitet
 * Den har ikke noe forhold til ev enhet som brukeren har en viss tidspunkt, som vil gjelde for tiltaksperioder
 *
 * Den finner "prioriterte" målgruppe/aktivitet-kombinasjoner, sånn at AAP trumfer Overgangsstønad
 */
object UtledStønadsperiode {

    fun utled(
        behandlingId: BehandlingId,
        vilkårperioder: List<Vilkårperiode>,
        medAntallAktivitetsdager: Boolean,
    ): List<Stønadsperiode> {
        return vilkårperioder
            .filtrerOppfylte()
            .snittMålgruppeAktivitet(medAntallAktivitetsdager)
            .tilStønadsperioder(behandlingId)
    }

    private fun List<Vilkårperiode>.snittMålgruppeAktivitet(medAntallAktivitetsdager: Boolean): Map<LocalDate, MålgruppeAktivitet> {
        val målgrupper = this.tilMålgrupper()
        val tilAktiviteter = this.tilAktiviteter(medAntallAktivitetsdager)
        val aktiviteter = tilAktiviteter.slåSammenType()

        return mutableMapOf<LocalDate, MålgruppeAktivitet>().apply {
            val map = this
            val finnDatoerForSnitt = finnDatoerForSnitt(målgrupper, aktiviteter)
            finnDatoerForSnitt
                .forEach { (målgruppeAktivitet, dato) ->
                    val prevValue = map[dato]
                    if (prevValue == null || prevValue < målgruppeAktivitet) {
                        map[dato] = målgruppeAktivitet
                    }
                }
        }
    }

    private fun List<Vilkårperiode>.filtrerOppfylte() =
        this.filter { it.resultat == ResultatVilkårperiode.OPPFYLT }

    /**
     * Itererer over sorterte målgrupper for å finne snitt med sorterte aktiviteter
     */
    private fun finnDatoerForSnitt(
        målgrupper: List<MålgruppeHolder>,
        aktiviteterPerType: Map<AktivitetType, List<AktivitetHolder>>,
    ): List<Pair<MålgruppeAktivitet, LocalDate>> {
        return målgrupper.flatMap { målgruppe ->
            målgruppe.type.gyldigeAktiviter.flatMap { typeAktivitet ->
                val verdier = aktiviteterPerType.verdier(typeAktivitet)
                verdier.snittDatoer(målgruppe)
                    .map { (dato, aktivitetsdager) ->
                        MålgruppeAktivitet(målgruppe.type, typeAktivitet, aktivitetsdager) to dato
                    }
            }
        }
    }

    /**
     * @return alle datoer for snittet mellom målgruppe og aktivitet
     */
    private fun List<AktivitetHolder>.snittDatoer(målgruppe: MålgruppeHolder): List<Pair<LocalDate, Int>> = this
        .mapNotNull { aktivitet ->
            aktivitet.snitt(målgruppe)?.let { it.alleDatoer().map { it to aktivitet.aktivitetsdager } }
        }
        .flatten()

    private fun List<Vilkårperiode>.tilMålgrupper(): List<MålgruppeHolder> = this
        .filter { it.type is MålgruppeType }
        .map { MålgruppeHolder(it.fom, it.tom, it.type as MålgruppeType) }

    private fun List<Vilkårperiode>.tilAktiviteter(medAntallAktivitetsdager: Boolean): List<AktivitetHolder> = this
        .filter { it.type is AktivitetType }
        .map { periode ->
            AktivitetHolder(
                fom = periode.fom,
                tom = periode.tom,
                type = periode.type as AktivitetType,
                aktivitetsdager = if (medAntallAktivitetsdager) {
                    periode.aktivitetsdager ?: 5
                } else {
                    5
                },
            )
        }

    /**
     * Slår sammen aktivitet av samme type og dato, og setter maks antall aktivitetsdager for gitt dato
     */
    private fun List<AktivitetHolder>.slåSammenType(): Map<AktivitetType, List<AktivitetHolder>> {
        return antallDagerPerAktivitetstypeOgDato()
            .slåSammenPerioderPerTypeOgAktivitetsdager()
    }

    private fun List<AktivitetHolder>.antallDagerPerAktivitetstypeOgDato(): Map<Pair<AktivitetType, LocalDate>, Int> =
        this.fold(mutableMapOf()) { acc, aktivitet ->
            aktivitet.alleDatoer().forEach { dato ->
                val key = Pair(aktivitet.type, dato)
                val antallAktivitetsdagerForDato = acc.getOrDefault(key, 0)
                acc[key] = minOf(5, aktivitet.aktivitetsdager + antallAktivitetsdagerForDato)
            }
            acc
        }

    private fun Map<Pair<AktivitetType, LocalDate>, Int>.slåSammenPerioderPerTypeOgAktivitetsdager()
            : Map<AktivitetType, List<AktivitetHolder>> =
        this.entries
            .map { (key, aktivitetsdager) ->
                AktivitetHolder(fom = key.second, tom = key.second, type = key.first, aktivitetsdager = aktivitetsdager)
            }
            .groupBy { it.type }
            .mapValues { (_, aktiviteter) ->
                aktiviteter.sorted().mergeSammenhengende { first, second ->
                    (first.overlapper(second) || first.tom.plusDays(1) == second.fom) &&
                            first.aktivitetsdager == second.aktivitetsdager
                }
            }

    private fun <T, R> Map<T, List<R>>.verdier(type: T): List<R> = this.getOrDefault(type, emptyList())

    private data class MålgruppeHolder(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val type: MålgruppeType,
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

        fun snitt(other: MålgruppeHolder): AktivitetHolder? {
            return if (this.overlapper(other)) {
                this.copy(fom = maxOf(this.fom, other.fom), tom = minOf(this.tom, other.tom))
            } else {
                null
            }
        }
    }
}
