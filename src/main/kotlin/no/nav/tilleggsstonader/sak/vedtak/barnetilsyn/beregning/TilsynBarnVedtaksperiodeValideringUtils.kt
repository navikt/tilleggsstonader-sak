package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode

object TilsynBarnVedtaksperiodeValideringUtils {
    fun validerUtgiftHeleVedtaksperioden(
        vedtaksperioder: List<Vedtaksperiode>,
        utgifter: Map<BarnId, List<UtgiftBeregning>>,
    ) {
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

    private fun erUtgiftperiodeSomInneholderVedtaksperiode(
        vedtaksperioder: List<Vedtaksperiode>,
        utgifter: Map<BarnId, List<UtgiftBeregning>>,
    ): Boolean {
        val sammenslåtteUtgiftPerioder =
            utgifter.values
                .flatMap {
                    it.map { Datoperiode(fom = it.fom.atDay(1), tom = it.tom.atEndOfMonth()) }
                }.sorted()
                .mergeSammenhengende { p1, p2 -> p1.overlapperEllerPåfølgesAv(p2) }

        return vedtaksperioder.any { vedtaksperiode ->
            sammenslåtteUtgiftPerioder.any { utgiftPeriode -> utgiftPeriode.inneholder(vedtaksperiode) }
        }
    }
}
