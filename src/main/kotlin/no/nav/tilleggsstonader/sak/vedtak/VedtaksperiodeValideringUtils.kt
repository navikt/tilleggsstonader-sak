package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.domain.PeriodeMedId
import java.time.LocalDate

object VedtaksperiodeValideringUtils {
    fun validerIngenEndringerFørRevurderFra(
        innsendteVedtaksperioder: List<PeriodeMedId>,
        vedtaksperioderForrigeBehandling: List<PeriodeMedId>?,
        revurderFra: LocalDate?,
    ) {
        if (revurderFra == null) return

        val innsendteVedtaksperioderFørRevurderFra = innsendteVedtaksperioder.filter { it.fom < revurderFra }
        val vedtaksperioderForrigeBehandlingFørRevurderFra =
            vedtaksperioderForrigeBehandling?.filter { it.fom < revurderFra }
        val innsendteVedtaksperioderMap = innsendteVedtaksperioderFørRevurderFra.associateBy { it.id }

        if (vedtaksperioderForrigeBehandlingFørRevurderFra.isNullOrEmpty()) {
            brukerfeilHvis(innsendteVedtaksperioder.any { it.fom < revurderFra }) {
                "Det er ikke tillat å legge til nye perioder før revurder fra dato"
            }
        } else {
            val vedtaksperioderForrigeBehandlingFørRevurderFraMedOppdatertTom =
                vedtaksperioderForrigeBehandlingFørRevurderFra.map { vedtaksperiodeForrigeBehandling ->
                    val tilhørendeInnsendtVedtaksperiode = innsendteVedtaksperioderMap[vedtaksperiodeForrigeBehandling.id]

                    if (tilhørendeInnsendtVedtaksperiode != null &&
                        // revurderFra.minusDays(1) tillater endringer dagen før revurder fra som trengs i opphør
                        tilhørendeInnsendtVedtaksperiode.tom >= revurderFra.minusDays(1) &&
                        vedtaksperiodeForrigeBehandling.tom >= revurderFra.minusDays(1)
                    ) {
                        vedtaksperiodeForrigeBehandling.kopier(
                            fom = vedtaksperiodeForrigeBehandling.fom,
                            tom = tilhørendeInnsendtVedtaksperiode.tom,
                        )
                    } else {
                        vedtaksperiodeForrigeBehandling
                    }
                }
            brukerfeilHvis(
                vedtaksperioderForrigeBehandlingFørRevurderFraMedOppdatertTom != innsendteVedtaksperioderFørRevurderFra,
            ) {
                "Det er ikke tillat å legge til, endre eller slette vedtaksperioder fra før revurder fra dato"
            }
        }
    }
}
