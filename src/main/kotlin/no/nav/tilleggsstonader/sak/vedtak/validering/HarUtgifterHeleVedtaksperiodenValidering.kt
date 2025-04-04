package no.nav.tilleggsstonader.sak.vedtak.validering

import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
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
    brukerfeilHvisIkke(
        erUtgiftperiodeSomInneholderVedtaksperiode(
            vedtaksperioder = vedtaksperioder,
            utgifter = utgifter,
        ),
    ) {
        "Kan ikke innvilge når det ikke finnes utgifter hele vedtaksperioden"
    }
}

private fun <T> erUtgiftperiodeSomInneholderVedtaksperiode(
    vedtaksperioder: List<Vedtaksperiode>,
    utgifter: Map<*, List<UtgiftBeregningType<T>>>,
): Boolean where T : Comparable<T>, T : Temporal {
    val sammenslåtteUtgiftPerioder =
        utgifter.values
            .flatMap { it.map { it.tilDatoPeriode() } }
            .sorted()
            .mergeSammenhengende { p1, p2 -> p1.overlapperEllerPåfølgesAv(p2) }

    return vedtaksperioder.any { vedtaksperiode ->
        sammenslåtteUtgiftPerioder.any { utgiftPeriode -> utgiftPeriode.inneholder(vedtaksperiode) }
    }
}
