package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.kontrakter.periode.avkortFraOgMed
import no.nav.tilleggsstonader.kontrakter.periode.avkortPerioderFør
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vedtak.domain.Avslag
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class VedtaksperiodeService(
    private val vedtakRepository: VedtakRepository,
) {
    fun finnNyeVedtaksperioderForOpphør(
        behandling: Saksbehandling,
        opphørsdato: LocalDate,
    ): List<Vedtaksperiode> {
        feilHvis(behandling.forrigeIverksatteBehandlingId == null) {
            "Kan ikke finne nye vedtaksperioder for opphør fordi behandlingen er en førstegangsbehandling"
        }

        val forrigeVedtaksperioder = finnVedtaksperioder(behandling.forrigeIverksatteBehandlingId)

        feilHvis(forrigeVedtaksperioder == null) {
            "Kan ikke opphøre fordi data fra forrige vedtak mangler"
        }

        // .minusDays(1) fordi dagen før opphørsdato blir siste dag i vedtaksperioden
        return forrigeVedtaksperioder.avkortFraOgMed(opphørsdato.minusDays(1))
    }

    fun detFinnesVedtaksperioderPåForrigeBehandling(saksbehandling: Saksbehandling): Boolean =
        finnVedtaksperioder(saksbehandling.forrigeIverksatteBehandlingId)?.isNotEmpty() == true

    private fun finnVedtaksperioder(behandlingId: BehandlingId?): List<Vedtaksperiode>? {
        if (behandlingId == null) return null
        val vedtak = vedtakRepository.findByIdOrNull(behandlingId)?.data
        return when (vedtak) {
            null -> null
            is InnvilgelseEllerOpphørTilsynBarn -> vedtak.vedtaksperioder
            is InnvilgelseEllerOpphørBoutgifter -> vedtak.vedtaksperioder
            is InnvilgelseEllerOpphørLæremidler -> vedtak.vedtaksperioder.map { it.tilFellesDomeneVedtaksperiode() }
            else ->
                error(
                    "Kan ikke hente vedtaksperioder når vedtak var ${vedtak.type}",
                )
        }
    }

    /**
     * Funksjon for å finne alle vedtaksperioder som er knyttet til en behandling.
     * I en revurdering lagrer man ned alle vedtaksperioder, også de før revurder-fra datoen.
     * Dvs. revurderer man fra 15. april, så vil også vedtaksperioder som eksisterer før dette lagres ned.
     *
     * For å finne vedtaksperiodene som faktisk ble vedtatt i behandlingen fjernes alle perioder før
     * revurder-fra datoen. Slik at man kun står igjen med vedtaksperioder som var redigerbare i den aktuelle behandlingen.
     */
    fun finnVedtaksperioderForBehandling(
        behandlingId: BehandlingId,
        revurdererFra: LocalDate?,
    ): List<Vedtaksperiode> {
        val vedtak = vedtakRepository.findByIdOrNull(behandlingId) ?: return emptyList()

        val vedtaksperioder: List<Vedtaksperiode> =
            when (vedtak.data) {
                is InnvilgelseEllerOpphørTilsynBarn -> vedtak.data.vedtaksperioder
                is InnvilgelseEllerOpphørLæremidler -> vedtak.data.vedtaksperioder.map { it.tilFellesDomeneVedtaksperiode() }
                is InnvilgelseEllerOpphørBoutgifter -> vedtak.data.vedtaksperioder
                is Avslag -> emptyList()
            }

        return vedtaksperioder.avkortPerioderFør(revurdererFra)
    }
}
