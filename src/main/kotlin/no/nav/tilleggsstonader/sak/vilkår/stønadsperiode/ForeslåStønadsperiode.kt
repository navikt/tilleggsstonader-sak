package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.util.tilFørsteDagIMåneden
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import java.time.LocalDate

object ForeslåStønadsperiode {

    fun finnStønadsperioder(
        vilkårperioder: Vilkårperioder,
        søknadsdato: LocalDate,
    ): List<StønadsperiodeDto> {
        val oppfylteVilkårsperioder = filtrerOppfylteVilkårsperioder(vilkårperioder)

        val filtrerteVilkårperioder = filterKombinasjonerAvMålgruppeOgAktivitet(oppfylteVilkårsperioder)

        brukerfeilHvis(filtrerteVilkårperioder.aktiviteter.isEmpty() || filtrerteVilkårperioder.målgrupper.isEmpty()) {
            "Det finnes ingen kombinasjon av aktiviteter og målgrupper som kan brukes til å lage perioder med overlapp"
        }

        brukerfeilHvis(
            filtrerteVilkårperioder.aktiviteter.size > 1 || filtrerteVilkårperioder.målgrupper.size > 1,
        ) {
            "Foreløpig håndterer vi kun én gyldig kombinasjon av aktivitet og målgruppe"
        }

        val stønadsperiode = finnOverlapp(
            filtrerteVilkårperioder.aktiviteter.first(),
            filtrerteVilkårperioder.målgrupper.first(),
        ).kuttTilMaksTreMånederTilbakeFraSøknadsdato(søknadsdato)

        return listOf(
            StønadsperiodeDto(
                fom = stønadsperiode.fom,
                tom = stønadsperiode.tom,
                målgruppe = filtrerteVilkårperioder.målgrupper.first().type as MålgruppeType,
                aktivitet = filtrerteVilkårperioder.aktiviteter.first().type as AktivitetType,
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

    private data class Stønadsperiode(val fom: LocalDate, val tom: LocalDate) {

        fun kuttTilMaksTreMånederTilbakeFraSøknadsdato(søknadsdato: LocalDate): Stønadsperiode {
            val førsteDagIMånedenForutForSøknadsdato = søknadsdato.minusMonths(3).tilFørsteDagIMåneden()

            brukerfeilHvis(this.tom.isBefore(førsteDagIMånedenForutForSøknadsdato)) {
                "Aktivitet og målgruppe ligger lengre enn tre månder tilbake i tid fra søknadsdato"
            }
            if (this.fom.isAfter(førsteDagIMånedenForutForSøknadsdato)) {
                return this
            }
            return Stønadsperiode(
                fom = førsteDagIMånedenForutForSøknadsdato,
                tom = this.tom,
            )
        }
    }
}
