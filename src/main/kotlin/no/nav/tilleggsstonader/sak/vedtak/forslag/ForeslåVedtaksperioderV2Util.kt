package no.nav.tilleggsstonader.sak.vedtak.forslag

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.kontrakter.periode.beregnSnitt
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.mergeSammenhengende
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import java.time.LocalDate
import java.util.UUID
import kotlin.collections.forEach

object ForeslåVedtaksperioderV2Util {
    fun foreslåPerioder(
        vilkårperioder: Vilkårperioder,
        vilkår: List<Vilkår>,
    ): List<Vedtaksperiode> {
        val forslag =
            foreslåPerioder(
                målgrupper = forenkledeMålgrupper(vilkårperioder),
                aktiviteter = vilkårperioder.aktiviteter.forenklet { it.type as AktivitetType },
                vilkår = vilkår.forenklet(),
            )
        brukerfeilHvis(forslag.isEmpty()) {
            "Fant ingen gyldig overlapp mellom aktivitet, målgruppe og utgiftsperiodene"
        }
        return forslag
    }

    fun foreslåPerioderUtenVilkår(vilkårperioder: Vilkårperioder): List<Vedtaksperiode> {
        val forslag =
            forslagVedtaksperiodeForInngangsvilkår(
                målgrupper = forenkledeMålgrupper(vilkårperioder),
                aktiviteter = vilkårperioder.aktiviteter.forenklet { it.type as AktivitetType },
            )
        brukerfeilHvis(forslag.isEmpty()) {
            "Fant ingen gyldig overlapp mellom aktivitet, målgruppe og utgiftsperiodene"
        }
        return forslag
    }

    private fun forenkledeMålgrupper(vilkårperioder: Vilkårperioder): List<ForenkletVilkårperiode<FaktiskMålgruppe>> =
        vilkårperioder.målgrupper
            .forenklet { (it.type as MålgruppeType).faktiskMålgruppe() }
            .values
            .flatten()

    fun foreslåPerioder(
        målgrupper: List<ForenkletVilkårperiode<FaktiskMålgruppe>>,
        aktiviteter: Map<AktivitetType, List<ForenkletVilkårperiode<AktivitetType>>>,
        vilkår: List<Datoperiode>,
    ): List<Vedtaksperiode> {
        val snittAvGyldigeKombinasjoner = forslagVedtaksperiodeForInngangsvilkår(målgrupper, aktiviteter)
        snittAvGyldigeKombinasjoner.forEach { snitt ->
            brukerfeilHvis(snittAvGyldigeKombinasjoner.any { it.id != snitt.id && it.overlapper(snitt) }) {
                // TODO bedre feilmelding. Har overlapp mellom flere kombinasjoner av målgruppe og aktivitet
                "Foreløpig klarer vi bare å foreslå perioder når målgruppe og aktivitet har ett sammenhengende overlapp. Her må du i stedet legge inn periodene manuelt."
            }
        }

        val forslag =
            snittAvGyldigeKombinasjoner
                .map { snitt -> vilkår.mapNotNull { snitt.beregnSnitt(it) } }
                .flatten()
                .sorted()
                .mergeSammenhengende()
                .map { it.copy(id = UUID.randomUUID()) }
        return forslag
    }

    fun forslagVedtaksperiodeForInngangsvilkår(
        målgrupper: List<ForenkletVilkårperiode<FaktiskMålgruppe>>,
        aktiviteter: Map<AktivitetType, List<ForenkletVilkårperiode<AktivitetType>>>,
    ): List<Vedtaksperiode> =
        målgrupper
            .flatMap { målgruppe ->
                målgruppe.type.gyldigeAktiviter
                    .mapNotNull { aktiviteter[it] }
                    .flatten()
                    .mapNotNull { målgruppe.snitt(it) }
            }.mergeSammenhengende()

    private fun List<Vilkår>.forenklet(): List<Datoperiode> =
        this
            .filter { it.resultat.erOppfylt() }
            .filter { it.fom != null && it.tom != null }
            .map { Datoperiode(fom = it.fom!!, tom = it.tom!!) }

    private inline fun <reified T : Enum<T>, V : Vilkårperiode> List<V>.forenklet(
        mapType: (V) -> T,
    ): Map<T, List<ForenkletVilkårperiode<T>>> =
        this
            .filter { it.resultat == ResultatVilkårperiode.OPPFYLT }
            .map {
                ForenkletVilkårperiode(
                    fom = it.fom,
                    tom = it.tom,
                    type = mapType(it),
                )
            }.groupBy { it.type }
            .mapValues { it.value.sorted().mergeSammenhengende(ForenkletVilkårperiode<T>::skalMerges) }

    private fun ForenkletVilkårperiode<FaktiskMålgruppe>.snitt(aktivitet: ForenkletVilkårperiode<AktivitetType>) =
        this.beregnSnitt(aktivitet)?.let {
            Vedtaksperiode(
                id = UUID.randomUUID(),
                fom = it.fom,
                tom = it.tom,
                målgruppe = this.type,
                aktivitet = aktivitet.type,
            )
        }

    data class ForenkletVilkårperiode<TYPE>(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val type: TYPE,
    ) : Periode<LocalDate>,
        Mergeable<LocalDate, ForenkletVilkårperiode<TYPE>>,
        KopierPeriode<ForenkletVilkårperiode<TYPE>> {
        init {
            validatePeriode()
        }

        fun skalMerges(other: ForenkletVilkårperiode<TYPE>): Boolean = this.type == other.type && this.overlapperEllerPåfølgesAv(other)

        override fun merge(other: ForenkletVilkårperiode<TYPE>): ForenkletVilkårperiode<TYPE> =
            this.copy(fom = minOf(this.fom, other.fom), tom = maxOf(this.tom, other.tom))

        override fun medPeriode(
            fom: LocalDate,
            tom: LocalDate,
        ): ForenkletVilkårperiode<TYPE> = this.copy(fom = fom, tom = tom)
    }
}
