package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.kontrakter.felles.førsteOverlappendePeriode
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Stønadsperiode

object VedtaksperiodeUtil {
    fun validerVedtaksperioder(
        vedtaksperioder: List<Vedtaksperiode>,
        stønadsperioder: List<Stønadsperiode>,
    ) {
        feilHvis(vedtaksperioder.size > 1) {
            "Foreløbig støtter vi kun beregning for læremidler med en vedtaksperiode"
        }

        val overlappendePeriode = vedtaksperioder.førsteOverlappendePeriode()
        if (overlappendePeriode != null) {
            brukerfeil("Periode=${overlappendePeriode.first.formatertPeriodeNorskFormat()} og ${overlappendePeriode.second.formatertPeriodeNorskFormat()} overlapper.")
        }

        feilHvis(
            vedtaksperioder.ingenOmfattesAvStønadsperioder(stønadsperioder),
        ) {
            "Vedtaksperiode er ikke innenfor en stønadsperiode"
        }
    }

    private fun List<Vedtaksperiode>.ingenOmfattesAvStønadsperioder(stønadsperioder: List<Stønadsperiode>): Boolean =
        any { vedtaksperiode ->
            stønadsperioder.none { it.inneholder(vedtaksperiode) }
        }
}
