package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.domain.PeriodeMedId
import java.time.LocalDate

object VedtaksperiodeValideringUtils {
    fun validerIngenEndringerFørRevurderFra(
        vedtaksperioder: List<PeriodeMedId>,
        vedtaksperioderForrigeBehandling: List<PeriodeMedId>?,
        revurderFra: LocalDate?,
    ) {
        if (revurderFra == null) return

        val vedtaksperioderFørRevurderFra = vedtaksperioder.filter { it.fom < revurderFra }
        val vedtaksperioderForrigeBehandlingFørRevurderFra =
            vedtaksperioderForrigeBehandling?.filter { it.fom < revurderFra }
        val vedtaksperioderMap = vedtaksperioderFørRevurderFra.associateBy { it.id }

        if (vedtaksperioderForrigeBehandlingFørRevurderFra.isNullOrEmpty()) {
            brukerfeilHvis(vedtaksperioder.any { it.fom < revurderFra }) {
                "Det er ikke tillat å legge til nye perioder før revurder fra dato"
            }
        } else {
            val vedtaksperioderForrigeBehandlingFørRevurderFraMedOppdatertTom =
                vedtaksperioderForrigeBehandlingFørRevurderFra.map { vedtaksperiodeForrigeBehandling ->
                    val nyVedtaksperiode = vedtaksperioderMap[vedtaksperiodeForrigeBehandling.id]

                    if (nyVedtaksperiode != null &&
                        // revurderFra.minusDays(1) tillater endringer dagen før revurder fra som trengs i opphør
                        nyVedtaksperiode.tom >= revurderFra.minusDays(1) &&
                        vedtaksperiodeForrigeBehandling.tom >= revurderFra.minusDays(1)
                    ) {
                        vedtaksperiodeForrigeBehandling.kopier(fom = vedtaksperiodeForrigeBehandling.fom, tom = nyVedtaksperiode.tom)
                    } else {
                        vedtaksperiodeForrigeBehandling
                    }
                }
            brukerfeilHvis(vedtaksperioderForrigeBehandlingFørRevurderFraMedOppdatertTom != vedtaksperioderFørRevurderFra) {
                "Det er ikke tillat å legg til, endre eller slette perioder fra før revurder fra dato"
            }
        }
    }
}
