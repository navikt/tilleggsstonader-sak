package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.automatisk

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import java.time.LocalDate

object AktivitetUtil {

    fun List<Vilkårperiode>.tilAktiviteter(
        medAntallAktivitetsdager: Boolean,
    ): Map<AktivitetType, List<AktivitetHolder>> = this
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
        }.slåSammenType()

    /**
     * Slår sammen aktivitet av samme type og dato, og setter maks antall aktivitetsdager for gitt dato
     */
    private fun List<AktivitetHolder>.slåSammenType(): Map<AktivitetType, List<AktivitetHolder>> = this
        .antallDagerPerAktivitetstypeOgDato()
        .slåSammenPerioderPerTypeOgAktivitetsdager()

    private fun List<AktivitetHolder>.antallDagerPerAktivitetstypeOgDato(): Map<Pair<AktivitetType, LocalDate>, Int> =
        this.fold(mutableMapOf()) { acc, aktivitet ->
            aktivitet.alleDatoer().forEach { dato ->
                val key = Pair(aktivitet.type, dato)
                val antallAktivitetsdagerForDato = acc.getOrDefault(key, 0)
                acc[key] = minOf(5, aktivitet.aktivitetsdager + antallAktivitetsdagerForDato)
            }
            acc
        }

    private fun Map<Pair<AktivitetType, LocalDate>, Int>.slåSammenPerioderPerTypeOgAktivitetsdager(): Map<AktivitetType, List<AktivitetHolder>> =
        this.entries
            .tilAktivitetHolder()
            .groupBy { it.type }
            .mapValues { (_, aktiviteter) -> aktiviteter.mergeSammenhengende() }

    private fun Set<Map.Entry<Pair<AktivitetType, LocalDate>, Int>>.tilAktivitetHolder() = this
        .map { (key, aktivitetsdager) ->
            AktivitetHolder(fom = key.second, tom = key.second, type = key.first, aktivitetsdager = aktivitetsdager)
        }

    private fun List<AktivitetHolder>.mergeSammenhengende() = this
        .sorted()
        .mergeSammenhengende { first, second ->
            (first.overlapper(second) || first.tom.plusDays(1) == second.fom) &&
                first.aktivitetsdager == second.aktivitetsdager
        }
}

data class AktivitetHolder(
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
