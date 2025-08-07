package no.nav.tilleggsstonader.sak.vedtak.forslag

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.forslag.ForeslåVedtaksperiodeFraVilkårperioder.foreslåVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.forslag.ForeslåVedtaksperioderBeholdIdUtil.beholdTidligereIdnForVedtaksperioder
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import java.time.LocalDate
import java.util.UUID

/**
 * Foreslå vedtaksperioder gir et forslag på perioder ut fra målgrupper, aktiviteter og vilkår.
 *
 * I en revurdering fylles hull i med nye vedtaksperioder.
 * Dvs både hull mellom 2 tidligere vedtaksperioder og hull etter tidligere vedtaksperioder.
 *
 * Hvis man har en [tidligsteEndring] endres ikke målgruppe/aktivitet før det datoet.
 * Hvis man mangler eller har en [tidligsteEndring] frem i tiden, fylles hull etter den siste tidligere vedtaksperioden.
 */
object ForeslåVedtaksperiode {
    fun finnVedtaksperiodeV2(
        vilkårperioder: Vilkårperioder,
        vilkår: List<Vilkår>,
        forrigeVedtaksperioder: List<Vedtaksperiode>,
        tidligsteEndring: LocalDate?,
    ): List<Vedtaksperiode> {
        val forslag = ForeslåVedtaksperioderV2Util.foreslåPerioder(vilkårperioder, vilkår)
        return beholdTidligereIdnForVedtaksperioder(forrigeVedtaksperioder, forslag, tidligsteEndring)
    }

    fun finnVedtaksperiodeUtenVilkårV2(
        vilkårperioder: Vilkårperioder,
        forrigeVedtaksperioder: List<Vedtaksperiode>,
        tidligsteEndring: LocalDate?,
    ): List<Vedtaksperiode> {
        val forslag = ForeslåVedtaksperioderV2Util.foreslåPerioderUtenVilkår(vilkårperioder)
        return beholdTidligereIdnForVedtaksperioder(forrigeVedtaksperioder, forslag, tidligsteEndring)
    }

    @Deprecated("Skal erstattes av finnVedtaksperiodeV2")
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

        val vedtaksperioder = finnOverlapp(forslagVedtaksperiode, sammenslåtteVilkår)

        brukerfeilHvis(vedtaksperioder.isEmpty()) {
            "Fant ingen gyldig overlapp mellom aktivitet, målgruppe og utgiftsperiodene"
        }

        return vedtaksperioder
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
        vilkår: List<Periode<LocalDate>>,
    ): List<Vedtaksperiode> =
        vilkår.filter { it.overlapper(forslagVedtaksperiode) }.map {
            Vedtaksperiode(
                id = UUID.randomUUID(),
                fom = maxOf(forslagVedtaksperiode.fom, it.fom),
                tom = minOf(forslagVedtaksperiode.tom, it.tom),
                målgruppe = forslagVedtaksperiode.målgruppe,
                aktivitet = forslagVedtaksperiode.aktivitet,
            )
        }
}
