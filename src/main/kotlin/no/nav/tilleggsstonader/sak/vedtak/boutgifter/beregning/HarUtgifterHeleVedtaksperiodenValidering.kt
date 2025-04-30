package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.UtgiftBeregningDato
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode

fun validerUtgiftHeleVedtaksperioden(
    vedtaksperioder: List<Vedtaksperiode>,
    utgifter: Map<TypeBoutgift, List<UtgiftBeregningDato>>,
) {
    val vedtaksperioderUtenOppfylteUtgifter =
        vedtaksperioder.filter { vedtaksperiode ->
            utgifter.slåSammenPåfølgende().none { it.inneholder(vedtaksperiode) }
        }

    brukerfeilHvisIkke(vedtaksperioderUtenOppfylteUtgifter.isEmpty()) {
        formulerFeilmelding(vedtaksperioderUtenOppfylteUtgifter)
    }
}

private fun Map<TypeBoutgift, Collection<UtgiftBeregningDato>>.slåSammenPåfølgende(): List<Datoperiode> =
    values
        .flatMap {
            it.map { Datoperiode(fom = it.fom, tom = it.tom) }
        }.sorted()
        .mergeSammenhengende { p1, p2 -> p1.overlapperEllerPåfølgesAv(p2) }

private fun formulerFeilmelding(perioderUtenOppfylteUtgifter: List<Vedtaksperiode>): String {
    val formatertePerioder = perioderUtenOppfylteUtgifter.map { it.formatertPeriodeNorskFormat() }
    val periodetekst =
        when (perioderUtenOppfylteUtgifter.size) {
            1 -> "Vedtaksperioden ${formatertePerioder.first()}"
            else -> "Vedtaksperiodene ${formatertePerioder.dropLast(1).joinToString(", ") + " og " + formatertePerioder.last()}"
        }
    return "$periodetekst mangler oppfylt utgift hele eller deler av perioden."
}
