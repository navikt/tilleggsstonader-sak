package no.nav.tilleggsstonader.sak.vedtak.forslag

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import java.time.LocalDate

object ForeslåVedtaksperiodeFraVilkårperioder {
    fun foreslåVedtaksperioder(vilkårperioder: Vilkårperioder): List<ForslagVedtaksperiodeFraVilkårperioder> {
        val oppfylteVilkårsperioder = filtrerOppfylteVilkårsperioder(vilkårperioder)

        val filtrerteVilkårperioder = filterKombinasjonerAvMålgruppeOgAktivitet(oppfylteVilkårsperioder)

        brukerfeilHvis(filtrerteVilkårperioder.aktiviteter.isEmpty() || filtrerteVilkårperioder.målgrupper.isEmpty()) {
            "Det finnes ingen kombinasjon av aktiviteter og målgrupper som kan brukes til å lage perioder med overlapp"
        }

        val sammenslåtteVilkårsperioder =
            Sammenslåtte(
                aktiviteter = mergeSammenhengende(filtrerteVilkårperioder.aktiviteter.forenkledeAktiviteter()),
                målgrupper = mergeSammenhengende(filtrerteVilkårperioder.målgrupper.forenkledeMålgrupper()),
            )

        brukerfeilHvis(
            sammenslåtteVilkårsperioder.aktiviteter.size > 1 || sammenslåtteVilkårsperioder.målgrupper.size > 1,
        ) {
            "Foreløpig klarer vi bare å foreslå perioder når målgruppe og aktivitet har ett sammenhengende overlapp. Her må du i stedet legge inn periodene manuelt."
        }

        val overlapp =
            finnOverlapp(
                sammenslåtteVilkårsperioder.aktiviteter.first(),
                sammenslåtteVilkårsperioder.målgrupper.first(),
            )

        return listOf(
            ForslagVedtaksperiodeFraVilkårperioder(
                fom = overlapp.fom,
                tom = overlapp.tom,
                målgruppe = sammenslåtteVilkårsperioder.målgrupper.first().type,
                aktivitet = sammenslåtteVilkårsperioder.aktiviteter.first().type,
            ),
        )
    }

    private fun filterKombinasjonerAvMålgruppeOgAktivitet(vilkårsperioder: Vilkårperioder): Vilkårperioder {
        val (målgrupper, aktiviteter) = vilkårsperioder

        val aktivitetstyper = aktiviteter.map { it.type as AktivitetType }.toSet()

        val filtrerteMålgrupper =
            målgrupper.filter { målgruppe ->
                (målgruppe.type as MålgruppeType).gyldigeAktiviter.any { it in aktivitetstyper }
            }
        val filtrerteAktiviteter =
            aktiviteter.filter { aktivitet ->
                målgrupper.any { målgruppe ->
                    (målgruppe.type as MålgruppeType).gyldigeAktiviter.any { gyldigeAktivitet ->
                        gyldigeAktivitet == aktivitet.type as AktivitetType
                    }
                }
            }
        return Vilkårperioder(filtrerteMålgrupper, filtrerteAktiviteter)
    }

    private fun filtrerOppfylteVilkårsperioder(vilkårperioder: Vilkårperioder): Vilkårperioder {
        val oppfylteAktiviter = vilkårperioder.aktiviteter.filter { it.resultat == ResultatVilkårperiode.OPPFYLT }
        val oppfylteMålgrupper = vilkårperioder.målgrupper.filter { it.resultat == ResultatVilkårperiode.OPPFYLT }
        return Vilkårperioder(aktiviteter = oppfylteAktiviter, målgrupper = oppfylteMålgrupper)
    }

    private fun finnOverlapp(
        aktivitet: Periode<LocalDate>,
        målgruppe: Periode<LocalDate>,
    ): Datoperiode =
        if (aktivitet.overlapper(målgruppe)) {
            Datoperiode(
                fom = maxOf(aktivitet.fom, målgruppe.fom),
                tom = minOf(aktivitet.tom, målgruppe.tom),
            )
        } else {
            brukerfeil("Fant ingen gyldig overlapp mellom gitte aktiviteter og målgrupper")
        }

    private fun <T : Enum<T>> mergeSammenhengende(vilkårperioder: List<ForenkletVilkårperiode<T>>): List<ForenkletVilkårperiode<T>> =
        vilkårperioder
            .sorted()
            .mergeSammenhengende(
                skalMerges = { v1, v2 -> v1.type == v2.type && v1.overlapperEllerPåfølgesAv(v2) },
                merge = { v1, v2 -> v1.copy(fom = minOf(v1.fom, v2.fom), tom = maxOf(v1.tom, v2.tom)) },
            )

    private fun List<VilkårperiodeAktivitet>.forenkledeAktiviteter() =
        this.map { ForenkletVilkårperiode(fom = it.fom, tom = it.tom, type = it.type as AktivitetType) }

    private fun List<VilkårperiodeMålgruppe>.forenkledeMålgrupper() =
        this.map {
            ForenkletVilkårperiode(
                fom = it.fom,
                tom = it.tom,
                type = (it.type as MålgruppeType).faktiskMålgruppe(),
            )
        }

    private data class Sammenslåtte(
        val aktiviteter: List<ForenkletVilkårperiode<AktivitetType>>,
        val målgrupper: List<ForenkletVilkårperiode<FaktiskMålgruppe>>,
    )

    private data class ForenkletVilkårperiode<TYPE>(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val type: TYPE,
    ) : Periode<LocalDate> {
        init {
            validatePeriode()
        }
    }
}
