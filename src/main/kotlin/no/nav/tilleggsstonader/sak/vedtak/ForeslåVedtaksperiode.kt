package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.forslag.ForeslåVedtaksperiodeFraVilkårperioder.foreslåVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.forslag.ForslagVedtaksperiodeFraVilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import java.time.LocalDate
import java.util.UUID

object ForeslåVedtaksperiode {
    fun finnVedtaksperiode(
        vilkårperioder: Vilkårperioder,
        vilkår: List<Vilkår>,
    ): List<Vedtaksperiode> {
        val forslagVedtaksperiode = foreslåVedtaksperioder(vilkårperioder).single()
        val oppfylteVilkår = vilkår.finnOppfylte()

        brukerfeilHvis(oppfylteVilkår.isEmpty()) {
            "Kunne ikke foreslå vedtaksperiode, ettersom det ikke er lagt inn noen utgiftsperioder der vilkårene er oppfylt."
        }

        val sammenslåtteVilkår =
            oppfylteVilkår.tilDatoPeriode().sorted().mergeSammenhengende { a, b -> a.overlapperEllerPåfølgesAv(b) }

        brukerfeilHvis(sammenslåtteVilkår.size > 1) {
            "Foreløpig klarer vi bare å foreslå perioder når vilkår har ett sammenhengende overlapp. Du må i stedet legge inn periodene manuelt."
        }

        val vedtaksperiode = finnOverlapp(forslagVedtaksperiode, sammenslåtteVilkår.first())

        return listOf(vedtaksperiode)
    }

    private fun List<Vilkår>.finnOppfylte() = filter { it.resultat === Vilkårsresultat.OPPFYLT }

    private fun List<Vilkår>.tilDatoPeriode() =
        mapNotNull {
            if (it.tom == null || it.fom == null) {
                null
            } else {
                Datoperiode(it.fom, it.tom)
            }
        }

    private fun finnOverlapp(
        forslagVedtaksperiode: ForslagVedtaksperiodeFraVilkårperioder,
        vilkår: Periode<LocalDate>,
    ): Vedtaksperiode =
        if (forslagVedtaksperiode.overlapper(vilkår)) {
            Vedtaksperiode(
                id = UUID.randomUUID(),
                fom = maxOf(forslagVedtaksperiode.fom, vilkår.fom),
                tom = minOf(forslagVedtaksperiode.tom, vilkår.tom),
                målgruppe = forslagVedtaksperiode.målgruppe,
                aktivitet = forslagVedtaksperiode.aktivitet,
            )
        } else {
            brukerfeil("Fant ingen gyldig overlapp mellom gitte aktiviteter, målgrupper og vilkår")
        }
}
