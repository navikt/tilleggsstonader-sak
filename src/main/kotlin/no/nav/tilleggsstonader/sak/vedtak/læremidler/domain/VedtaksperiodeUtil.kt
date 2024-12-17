package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.kontrakter.felles.førsteOverlappendePeriode
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag

object VedtaksperiodeUtil {
    fun validerVedtaksperioder(
        vedtaksperioder: List<Vedtaksperiode>,
        stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>,
    ) {
        brukerfeilHvis(vedtaksperioder.erFlereISammeKalenderMåned()) {
            "Foreløbig støtter vi kun en vedtaksperiode per kalendermåned"
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

    private fun List<Vedtaksperiode>.erFlereISammeKalenderMåned(): Boolean {
        return this.flatMap {
            val fomMåned = it.fom.toYearMonth()
            val tomMåned = it.tom.toYearMonth()
            if (fomMåned == tomMåned) listOf(fomMåned) else listOf(fomMåned, tomMåned)
        }.groupingBy { it }
            .eachCount()
            .any { it.value > 1 }
    }
}
