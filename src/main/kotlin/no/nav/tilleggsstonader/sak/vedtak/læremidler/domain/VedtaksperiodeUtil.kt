package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.kontrakter.felles.påfølgesAv
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Stønadsperiode

object VedtaksperiodeUtil {
    fun validerVedtaksperioder(
        vedtaksperioder: List<Vedtaksperiode>,
        stønadsperioder: List<Stønadsperiode>,
    ) {
        feilHvis(vedtaksperioder.overlapper()) {
            "Vedtaksperioder overlapper"
        }

        feilHvis(
            vedtaksperioder.ingenOmfattesAvStønadsperioder(stønadsperioder),
        ) {
            "Vedtaksperiode er ikke innenfor en stønadsperiode"
        }
    }

    private fun List<Stønadsperiode>.slåSammenSammenhengende(): List<Stønadsperiode> {
        return this.mergeSammenhengende(
            skalMerges = { a, b -> a.påfølgesAv(b) },
            merge = { a, b -> a.copy(tom = b.tom) },
        )
    }

    private fun List<Vedtaksperiode>.ingenOmfattesAvStønadsperioder(stønadsperioder: List<Stønadsperiode>): Boolean {
        return this.any { vedtaksperiode ->
            stønadsperioder.slåSammenSammenhengende().none { it.inneholder(vedtaksperiode) }
        }
    }
}
