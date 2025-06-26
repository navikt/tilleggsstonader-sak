package no.nav.tilleggsstonader.sak.vedtak.validering

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.Avslag
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringUtils.validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringUtils.validerEnkeltperiode
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringUtils.validerIngenOverlappMellomVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringUtils.validerVedtaksperioderEksisterer
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.mergeSammenhengendeOppfylteAktiviteter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.mergeSammenhengendeOppfylteMålgrupper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class VedtaksperiodeValideringService(
    private val vilkårperiodeService: VilkårperiodeService,
    private val vedtakRepository: VedtakRepository,
) {
    /**
     * Felles format på Vedtaksperiode inneholder ennå ikke status så mapper til felles format for å kunne validere
     * vedtaksperioder på lik måte
     */
    fun validerVedtaksperioderLæremidler(
        vedtaksperioder: List<no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode>,
        behandling: Saksbehandling,
        typeVedtak: TypeVedtak,
        tidligsteEndring: LocalDate?,
    ) {
        validerVedtaksperioder(vedtaksperioder.tilFellesVedtaksperiode(), behandling, typeVedtak, tidligsteEndring)
    }

    fun validerVedtaksperioder(
        vedtaksperioder: List<Vedtaksperiode>,
        behandling: Saksbehandling,
        typeVedtak: TypeVedtak,
        tidligsteEndring: LocalDate?,
    ) {
        if (typeVedtak != TypeVedtak.OPPHØR) {
            validerVedtaksperioderEksisterer(vedtaksperioder)
        }
        validerIngenOverlappMellomVedtaksperioder(vedtaksperioder)

        validerVedtaksperioderMotVilkårperioder(behandling, vedtaksperioder)

        validerIngenEndringerFørRevurderFra(
            innsendteVedtaksperioder = vedtaksperioder,
            vedtaksperioderForrigeBehandling = hentForrigeVedtaksperioder(behandling),
            revurderFra = tidligsteEndring,
        )
    }

    private fun validerVedtaksperioderMotVilkårperioder(
        behandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
    ) {
        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(behandling.id)
        validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett(vilkårperioder, vedtaksperioder)
        val målgrupper = vilkårperioder.målgrupper.mergeSammenhengendeOppfylteMålgrupper()
        val aktiviteter = vilkårperioder.aktiviteter.mergeSammenhengendeOppfylteAktiviteter()

        vedtaksperioder.forEach {
            validerEnkeltperiode(
                vedtaksperiode = it,
                målgruppePerioderPerType = målgrupper,
                aktivitetPerioderPerType = aktiviteter,
            )
        }
    }

    private fun hentForrigeVedtaksperioder(behandling: Saksbehandling): List<Vedtaksperiode>? =
        behandling.forrigeIverksatteBehandlingId?.let {
            when (val forrigeVedtak = vedtakRepository.findByIdOrNull(it)?.data) {
                is InnvilgelseEllerOpphørTilsynBarn -> forrigeVedtak.vedtaksperioder
                is InnvilgelseEllerOpphørBoutgifter -> forrigeVedtak.vedtaksperioder
                is InnvilgelseEllerOpphørLæremidler -> forrigeVedtak.vedtaksperioder.tilFellesVedtaksperiode()
                is Avslag -> null
                else -> error("Håndterer ikke ${forrigeVedtak?.javaClass?.simpleName}")
            }
        }
}

/**
 * For å kunne gjenbruke validering er det ønskelig å mappe vedtaksperiode til felles format for vedtaksperioder
 */
private fun List<no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode>.tilFellesVedtaksperiode() =
    this.map {
        Vedtaksperiode(
            id = it.id,
            fom = it.fom,
            tom = it.tom,
            aktivitet = it.aktivitet,
            målgruppe = it.målgruppe,
        )
    }
