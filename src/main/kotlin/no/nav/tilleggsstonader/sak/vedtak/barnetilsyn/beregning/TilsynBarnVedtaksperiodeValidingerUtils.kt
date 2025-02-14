package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode

object TilsynBarnVedtaksperiodeValidingerUtils {
    // TODO
    // valider gyldig målgruppe
    // valider gyldig aktivitet
    // valider gyldig kobinasjon av aktivitet og målgruppe

    fun validerVedtaksperioder(vedtaksperioder: List<Vedtaksperiode>) {
        validerIngenOverlappMellomVedtaksperioder(vedtaksperioder)
    }

    private fun validerIngenOverlappMellomVedtaksperioder(vedtaksperioder: List<Vedtaksperiode>) {
        brukerfeilHvis(vedtaksperioder.overlapper()) {
            "Vedtaksperioder kan ikke overlappe"
        }
    }
}
