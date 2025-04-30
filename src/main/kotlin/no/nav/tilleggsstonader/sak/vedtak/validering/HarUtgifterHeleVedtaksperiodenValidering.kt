package no.nav.tilleggsstonader.sak.vedtak.validering

import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.UtgiftBeregningType
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import java.time.temporal.Temporal

fun <T> validerUtgiftHeleVedtaksperioden(
    vedtaksperioder: List<Vedtaksperiode>,
    utgifter: Map<*, List<UtgiftBeregningType<T>>>,
) where T : Comparable<T>, T : Temporal {
    if (vedtaksperioder.isEmpty()) {
        return
    }

    val vedtaksperioderUtenOppfylteUtgifter =
        erUtgiftperiodeSomInneholderVedtaksperiode(
            vedtaksperioder = vedtaksperioder,
            utgifter = utgifter,
        )

    brukerfeilHvisIkke(vedtaksperioderUtenOppfylteUtgifter.isEmpty()) {
        formulerFeilmelding(vedtaksperioderUtenOppfylteUtgifter)
    }
}

private fun <T> erUtgiftperiodeSomInneholderVedtaksperiode(
    vedtaksperioder: List<Vedtaksperiode>,
    utgifter: Map<*, List<UtgiftBeregningType<T>>>,
): List<Vedtaksperiode> where T : Comparable<T>, T : Temporal {
    val sammenslåtteUtgiftPerioder =
        utgifter.values
            .flatten()
            .map { it.tilDatoPeriode() }
            .sorted()
            .mergeSammenhengende { p1, p2 -> p1.overlapperEllerPåfølgesAv(p2) }

    return vedtaksperioder.filter { vedtaksperiode ->
        sammenslåtteUtgiftPerioder.none { it.inneholder(vedtaksperiode) }
    }
}

private fun formulerFeilmelding(perioderUtenOppfylteUtgifter: List<Vedtaksperiode>): String {
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
