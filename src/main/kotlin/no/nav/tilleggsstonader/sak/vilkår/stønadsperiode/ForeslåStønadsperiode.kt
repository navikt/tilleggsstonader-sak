package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.GeneriskVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurdering
import java.time.LocalDate

object ForeslåStønadsperiode {

    fun finnStønadsperioder(
        vilkårperioder: Vilkårperioder,
    ): List<StønadsperiodeDto> {
        val oppfylteVilkårsperioder = filtrerOppfylteVilkårsperioder(vilkårperioder)

        val filtrerteVilkårperioder = filterKombinasjonerAvMålgruppeOgAktivitet(oppfylteVilkårsperioder)

        brukerfeilHvis(filtrerteVilkårperioder.aktiviteter.isEmpty() || filtrerteVilkårperioder.målgrupper.isEmpty()) {
            "Det finnes ingen kombinasjon av aktiviteter og målgrupper som kan brukes til å lage perioder med overlapp"
        }

        val sammenslåtteVilkårsperioder = Vilkårperioder(
            aktiviteter = slåSammenVilkårsperioderSomErLikeEtterHverandre(filtrerteVilkårperioder.aktiviteter),
            målgrupper = slåSammenVilkårsperioderSomErLikeEtterHverandre(filtrerteVilkårperioder.målgrupper),
        )

        brukerfeilHvis(
            sammenslåtteVilkårsperioder.aktiviteter.size > 1 || sammenslåtteVilkårsperioder.målgrupper.size > 1,
        ) {
            "Foreløpig håndterer vi kun tilfellet der målgruppe og aktivitet har ett sammenhengende overlapp."
        }

        val stønadsperiode = finnOverlapp(
            sammenslåtteVilkårsperioder.aktiviteter.first(),
            sammenslåtteVilkårsperioder.målgrupper.first(),
        )

        return listOf(
            StønadsperiodeDto(
                fom = stønadsperiode.fom,
                tom = stønadsperiode.tom,
                målgruppe = sammenslåtteVilkårsperioder.målgrupper.first().type as MålgruppeType,
                aktivitet = sammenslåtteVilkårsperioder.aktiviteter.first().type as AktivitetType,
                status = null,
            ),
        )
    }

    private fun filterKombinasjonerAvMålgruppeOgAktivitet(
        vilkårsperioder: Vilkårperioder,
    ): Vilkårperioder {
        val (målgrupper, aktiviteter) = vilkårsperioder

        val aktivitetstyper = aktiviteter.map { it.type as AktivitetType }.toSet()

        val filtrerteMålgrupper = målgrupper.filter { målgruppe ->
            (målgruppe.type as MålgruppeType).gyldigeAktiviter.any { it in aktivitetstyper }
        }
        val filtrerteAktiviteter = aktiviteter.filter { aktivitet ->
            målgrupper.any { målgruppe ->
                (målgruppe.type as MålgruppeType).gyldigeAktiviter.any { gyldigeAktivitet ->
                    gyldigeAktivitet == aktivitet.type as AktivitetType
                }
            }
        }
        return Vilkårperioder(filtrerteMålgrupper, filtrerteAktiviteter)
    }

    private fun filtrerOppfylteVilkårsperioder(vilkårperioder: Vilkårperioder): Vilkårperioder {
        val oppfylteAktiviter = vilkårperioder.aktiviteter.filter {
            it.resultat == ResultatVilkårperiode.OPPFYLT
        }
        val oppfylteMålgrupper = vilkårperioder.målgrupper.filter {
            it.resultat == ResultatVilkårperiode.OPPFYLT
        }
        return Vilkårperioder(aktiviteter = oppfylteAktiviter, målgrupper = oppfylteMålgrupper)
    }

    private fun finnOverlapp(aktivitet: Periode<LocalDate>, målgruppe: Periode<LocalDate>): Stønadsperiode {
        return if (aktivitet.overlapper(målgruppe)) {
            Stønadsperiode(
                fom = maxOf(aktivitet.fom, målgruppe.fom),
                tom = minOf(aktivitet.tom, målgruppe.tom),
            )
        } else {
            brukerfeil("Fant ingen gyldig overlapp mellom gitte aktiviteter og målgrupper")
        }
    }

    private inline fun <T : FaktaOgVurdering> slåSammenVilkårsperioderSomErLikeEtterHverandre(
        vilkårperioder: List<GeneriskVilkårperiode<T>>,
    ): List<GeneriskVilkårperiode<T>> {
        return vilkårperioder
            .sorted()
            .mergeSammenhengende(
                skalMerges = { v1, v2 -> v1.type == v2.type && v1.overlapperEllerPåfølgesAv(v2) },
                merge = { v1, v2 -> v1.copy(fom = minOf(v1.fom, v2.fom), tom = maxOf(v1.tom, v2.tom)) },
            )
    }

    private data class Stønadsperiode(val fom: LocalDate, val tom: LocalDate)
}
