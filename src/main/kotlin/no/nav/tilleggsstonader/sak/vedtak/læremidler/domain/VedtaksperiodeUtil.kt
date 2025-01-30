package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.førsteOverlappendePeriode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.påfølgesAv
import no.nav.tilleggsstonader.kontrakter.periode.beregnSnitt
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag
import java.time.LocalDate

object VedtaksperiodeUtil {
    fun validerVedtaksperioder(
        vedtaksperioder: List<Vedtaksperiode>,
        vedtaksperioderForrigeBehandling: List<Vedtaksperiode>?,
        stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>,
        revurderFra: LocalDate?,
    ) {
        validerIngenOverlappendeVedtaksperioder(vedtaksperioder)
        validerVedtaksperiodeOmfattesAvStønadsperioder(vedtaksperioder, stønadsperioder)
        validerIngenEndringerFørRevurderFra(vedtaksperioder, vedtaksperioderForrigeBehandling, revurderFra)
    }

    fun validerIngenEndringerFørRevurderFra(
        vedtaksperioder: List<Vedtaksperiode>,
        vedtaksperioderForrigeBehandling: List<Vedtaksperiode>?,
        revurderFra: LocalDate?,
    ) {
        if (revurderFra == null || vedtaksperioderForrigeBehandling.isNullOrEmpty()) return

        val vedtaksperioderFørRevuderFra = vedtaksperioder.filter { it.fom < revurderFra }
        val vedtaksperioderForrigeBehandlingFørRevurderFra =
            vedtaksperioderForrigeBehandling.filter { it.fom < revurderFra }

        brukerfeilHvis(vedtaksperioderForrigeBehandlingFørRevurderFra != vedtaksperioderFørRevuderFra) {
            "Endringer i vedtaksperioder før revurderFra er ikke tillatt"
        }
    }

    private fun validerIngenOverlappendeVedtaksperioder(vedtaksperioder: List<Vedtaksperiode>) {
        val overlappendePeriode = vedtaksperioder.førsteOverlappendePeriode()
        if (overlappendePeriode != null) {
            brukerfeil(
                "Periode=${overlappendePeriode.first.formatertPeriodeNorskFormat()} og ${overlappendePeriode.second.formatertPeriodeNorskFormat()} overlapper.",
            )
        }
    }

    private fun validerVedtaksperiodeOmfattesAvStønadsperioder(
        vedtaksperioder: List<Vedtaksperiode>,
        stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>,
    ) {
        brukerfeilHvis(vedtaksperioder.ingenOmfattesAvStønadsperioder(stønadsperioder)) {
            "Vedtaksperiode er ikke innenfor en overlappsperiode"
        }
    }

    /**
     * Når vi sjekker om vedtaksperioder omfattas av stønadsperioder så er vi ikke avhengig av hvilken målgruppe/aktivitet de har
     * Alle må omfattes av stønadsperiode
     */
    private fun List<Vedtaksperiode>.ingenOmfattesAvStønadsperioder(stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>): Boolean {
        val sammenslåtteStønadsperioder =
            stønadsperioder
                .sorted()
                .map { Datoperiode(fom = it.fom, tom = it.tom) }
                .mergeSammenhengende(
                    { s1, s2 -> s1.påfølgesAv(s2) },
                    { s1, s2 -> Datoperiode(fom = s1.fom, tom = s2.tom) },
                )
        return any { vedtaksperiode ->
            sammenslåtteStønadsperioder.none { it.inneholder(vedtaksperiode) }
        }
    }

    /**
     * Finner
     */
    fun vedtaksperioderInnenforLøpendeMåned(
        vedtaksperioder: List<Vedtaksperiode>,
        beregningsresultatTilReberegning: BeregningsresultatForMåned,
    ): List<Vedtaksperiode> =
        vedtaksperioder
            .mapNotNull { vedtaksperiode ->
                vedtaksperiode.beregnSnitt(beregningsresultatTilReberegning)
            }
}
