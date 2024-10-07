package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.automatisk

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode
import java.time.LocalDate

/**
 * Util for å mappe, slå sammen og mappe til stønadsperioder
 */
object StønadsperiodeMapper {

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
                    .mergeSammenhengende { a, b -> a.tom.plusDays(1) == b.tom }
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
