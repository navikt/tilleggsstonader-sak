package no.nav.tilleggsstonader.sak.vedtak.validering

import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.domain.PeriodeMedId
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeStatus
import java.time.LocalDate

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
                val tilhørendeInnsendtVedtaksperiode =
                    innsendteVedtaksperioderMap[vedtaksperiodeForrigeBehandling.id]

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
            vedtaksperioderForrigeBehandlingFørRevurderFraMedOppdatertTom.erUlik(
                innsendteVedtaksperioderFørRevurderFra,
            ),
        ) {
            "Det er ikke tillat å legge til, endre eller slette vedtaksperioder fra før revurder fra dato"
        }
    }
}

private fun List<PeriodeMedId>.erUlik(other: List<PeriodeMedId>) = this.toSet() != other.toSet()
