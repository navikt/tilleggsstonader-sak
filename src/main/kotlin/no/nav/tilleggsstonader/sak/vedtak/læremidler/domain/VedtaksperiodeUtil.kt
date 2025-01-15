package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.kontrakter.felles.førsteOverlappendePeriode
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag

object VedtaksperiodeUtil {
    fun validerVedtaksperioder(
        vedtaksperioder: List<Vedtaksperiode>,
        stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>,
    ) {
        val overlappendePeriode = vedtaksperioder.førsteOverlappendePeriode()
        if (overlappendePeriode != null) {
            brukerfeil("Periode=${overlappendePeriode.first.formatertPeriodeNorskFormat()} og ${overlappendePeriode.second.formatertPeriodeNorskFormat()} overlapper.")
        }

        brukerfeilHvis(
            vedtaksperioder.ingenOmfattesAvStønadsperioder(stønadsperioder),
        ) {
            "Vedtaksperiode er ikke innenfor en overlappsperiode"
        }
    }

    private fun List<Vedtaksperiode>.ingenOmfattesAvStønadsperioder(stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>): Boolean =
        any { vedtaksperiode ->
            stønadsperioder.none { it.inneholder(vedtaksperiode) }
        }
}
