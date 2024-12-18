package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.kontrakter.felles.førsteOverlappendePeriode
import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerPeriodeUtil.splitPerLøpendeMåneder

object VedtaksperiodeUtil {
    fun validerVedtaksperioder(
        vedtaksperioder: List<Vedtaksperiode>,
        stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>,
    ) {
        brukerfeilHvis(vedtaksperioder.erFlereISammeLøpendeMåned()) {
            "Foreløbig støtter vi kun en vedtaksperiode per løpende måned"
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

    private fun List<Vedtaksperiode>.ingenOmfattesAvStønadsperioder(stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>): Boolean =
        any { vedtaksperiode ->
            stønadsperioder.none { it.inneholder(vedtaksperiode) }
        }

    private fun List<Vedtaksperiode>.erFlereISammeLøpendeMåned(): Boolean {
        return flatMap {
            it.splitPerLøpendeMåneder({ fom, tom -> Vedtaksperiode(fom, tom) }, false)
        }.overlapper()
    }
}
