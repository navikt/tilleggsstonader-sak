package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.VedtaksperiodeValideringUtils.validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett
import no.nav.tilleggsstonader.sak.vedtak.VedtaksperiodeValideringUtils.validerEnkeltperiode
import no.nav.tilleggsstonader.sak.vedtak.VedtaksperiodeValideringUtils.validerIngenOverlappMellomVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.VedtaksperiodeValideringUtils.validerVedtaksperioderEksisterer
import no.nav.tilleggsstonader.sak.vedtak.domain.Avslag
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.validerIngenEndringerFørRevurderFra
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.mergeSammenhengendeOppfylteAktiviteter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.mergeSammenhengendeOppfylteMålgrupper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class TilsynBarnVedtaksperiodeValidingerService(
    private val vilkårperiodeService: VilkårperiodeService,
    private val vedtakRepository: VedtakRepository,
) {
    fun validerVedtaksperioder(
        vedtaksperioder: List<Vedtaksperiode>,
        behandling: Saksbehandling,
        utgifter: Map<BarnId, List<UtgiftBeregning>>,
        typeVedtak: TypeVedtak,
    ) {
        if (typeVedtak != TypeVedtak.OPPHØR) {
            validerVedtaksperioderEksisterer(vedtaksperioder)
        }
        validerIngenOverlappMellomVedtaksperioder(vedtaksperioder)
        validerUtgiftHeleVedtaksperioden(vedtaksperioder, utgifter)

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

        validerIngenEndringerFørRevurderFra(
            innsendteVedtaksperioder = vedtaksperioder,
            vedtaksperioderForrigeBehandling = hentForrigeVedtaksperioder(behandling),
            revurderFra = behandling.revurderFra,
        )
    }

    private fun hentForrigeVedtaksperioder(behandling: Saksbehandling): List<Vedtaksperiode>? =
        behandling.forrigeIverksatteBehandlingId?.let {
            when (val forrigeVedtak = vedtakRepository.findByIdOrNull(it)?.data) {
                is InnvilgelseTilsynBarn -> forrigeVedtak.vedtaksperioder
                is OpphørTilsynBarn -> forrigeVedtak.vedtaksperioder
                is Avslag -> null
                else -> error("Håndterer ikke ${forrigeVedtak?.javaClass?.simpleName}")
            }
        }
}
