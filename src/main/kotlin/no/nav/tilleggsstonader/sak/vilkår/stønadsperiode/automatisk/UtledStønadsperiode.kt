package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.automatisk

import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.automatisk.AktivitetUtil.tilAktiviteter
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.automatisk.MålgruppeUtil.tilMålgrupper
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.automatisk.StønadsperiodeMapper.tilStønadsperioder
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
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
        val aktiviteter = this.tilAktiviteter(medAntallAktivitetsdager)

        return finnDatoerForSnitt(målgrupper, aktiviteter)
            .groupingBy { it.second }
            .aggregate { _, prev: MålgruppeAktivitet?, (målgruppeAktivitet, _), _ ->
                if (prev == null || prev < målgruppeAktivitet) {
                    målgruppeAktivitet
                } else {
                    prev
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

    private fun <T, R> Map<T, List<R>>.verdier(type: T): List<R> = this.getOrDefault(type, emptyList())
}
