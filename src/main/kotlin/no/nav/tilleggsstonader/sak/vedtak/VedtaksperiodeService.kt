package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.kontrakter.periode.avkortFraOgMed
import no.nav.tilleggsstonader.kontrakter.periode.avkortPerioderFør
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vedtak.domain.Avslag
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class VedtaksperiodeService(
    private val vilkårperiodeService: VilkårperiodeService,
    private val vilkårService: VilkårService,
    private val vedtakRepository: VedtakRepository,
    private val behandlingService: BehandlingService,
) {
    fun foreslåPerioder(behandlingId: BehandlingId): List<Vedtaksperiode> {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        brukerfeilHvis(detFinnesVedtaksperioderPåForrigeBehandling(saksbehandling)) {
            "Kan ikke foreslå vedtaksperioder fordi det finnes lagrede vedtaksperioder fra en tidligere behandling"
        }

        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)
        val vilkår = vilkårService.hentVilkår(behandlingId)

        return ForeslåVedtaksperiode.finnVedtaksperiode(
            vilkårperioder = vilkårperioder,
            vilkår = vilkår,
        )
    }

    fun finnNyeVedtaksperioderForOpphør(behandling: Saksbehandling): List<Vedtaksperiode> {
        feilHvis(behandling.forrigeIverksatteBehandlingId == null) {
            "Kan ikke finne nye vedtaksperioder for opphør fordi behandlingen er en førstegangsbehandling"
        }
        feilHvis(behandling.revurderFra == null) {
            "Kan ikke finne nye vedtaksperioder for opphør fordi revurder fra dato mangler"
        }

        val forrigeVedtaksperioder = finnVedtaksperioder(behandling.forrigeIverksatteBehandlingId)

        feilHvis(forrigeVedtaksperioder == null) {
            "Kan ikke opphøre fordi data fra forrige vedtak mangler"
        }

        // .minusDays(1) fordi dagen før revurder fra blir siste dag i vedtaksperioden
        return forrigeVedtaksperioder.avkortFraOgMed(behandling.revurderFra.minusDays(1))
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

        val vedtaksperioder: List<Vedtaksperiode>? =
            when (vedtak.data) {
                is InnvilgelseEllerOpphørTilsynBarn -> vedtak.data.vedtaksperioder
                is InnvilgelseEllerOpphørLæremidler -> vedtak.data.vedtaksperioder.map { it.tilFellesDomeneVedtaksperiode() }
                is InnvilgelseEllerOpphørBoutgifter -> vedtak.data.vedtaksperioder
                is Avslag -> emptyList()
            }

        return vedtaksperioder?.avkortPerioderFør(revurdererFra) ?: emptyList()
    }
}
