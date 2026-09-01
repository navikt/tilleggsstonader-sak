package no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.libs.feil.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.VilkårReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning.Companion.mergeSammenhengende as mergeSammenhengendeReiseOppstartAvslutningHjemreise

object ReiseOppstartAvslutningHjemreiseValidering {
    fun List<VilkårReiseOppstartAvslutningHjemreise>.slåSammenPåfølgende(): List<Datoperiode> =
        this
            .map { Datoperiode(fom = it.fom, tom = it.tom) }
            .sorted()
            .mergeSammenhengende { p1, p2 ->
                p1.overlapperEllerPåfølgesAv(p2)
            }

    fun validerUtgiftHeleVedtaksperioden(
        vedtaksperioder: List<Vedtaksperiode>,
        utgifter: List<VilkårReiseOppstartAvslutningHjemreise>,
    ) {
        val vedtaksperioderUtenOppfylteUtgifter =
            vedtaksperioder.filter { vedtaksperiode ->
                utgifter.slåSammenPåfølgende().none { it.inneholder(vedtaksperiode) }
            }

        brukerfeilHvisIkke(vedtaksperioderUtenOppfylteUtgifter.isEmpty()) {
            formulerFeilmelding(vedtaksperioderUtenOppfylteUtgifter)
        }
    }

    fun formulerFeilmelding(perioderUtenOppfylteUtgifter: List<Vedtaksperiode>): String {
        val formatertePerioder = perioderUtenOppfylteUtgifter.map { it.formatertPeriodeNorskFormat() }
        val periodetekst =
            when (perioderUtenOppfylteUtgifter.size) {
                1 -> "Vedtaksperioden ${formatertePerioder.first()}"
                else ->
                    "Vedtaksperiodene ${
                        formatertePerioder.dropLast(1).joinToString(", ") + " og " + formatertePerioder.last()
                    }"
            }
        return "$periodetekst mangler oppfylt utgift hele eller deler av perioden."
    }

    fun validerUtgifterStrekkerSegUtenforVedtaksperiodene(
        utgifter: List<VilkårReiseOppstartAvslutningHjemreise>,
        vedtaksperioder: List<VedtaksperiodeBeregning>,
    ) {
        val alleUtgifterErInnenforVedtaksperioder =
            utgifter.all { utgift ->
                vedtaksperioder
                    .mergeSammenhengendeReiseOppstartAvslutningHjemreise()
                    .any { it.inneholder(utgift) }
            }

        brukerfeilHvisIkke(alleUtgifterErInnenforVedtaksperioder) {
            "Vi har foreløpig ikke støtte for å beregne når reiseperioder strekker seg utenfor vedtaksperiodene."
        }
    }

    fun List<VilkårReiseOppstartAvslutningHjemreise>.filtrerBortUtgifterSomIkkeOverlapperVedtaksperioder(
        vedtaksperioder: List<VedtaksperiodeBeregning>,
    ): List<VilkårReiseOppstartAvslutningHjemreise> =
        filter { vilkår ->
            vedtaksperioder.any { it.overlapper(vilkår) }
        }
}
