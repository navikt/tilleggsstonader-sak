package no.nav.tilleggsstonader.sak.vedtak.validering

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringUtils.validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringUtils.validerEnkeltperiode
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringUtils.validerIngenOverlappMellomVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringUtils.validerVedtaksperioderEksisterer
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.mergeSammenhengendeOppfylteAktiviteter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.mergeSammenhengendeOppfylteMålgrupper
import org.springframework.stereotype.Service

@Service
class VedtaksperiodeValideringService(
    private val vilkårperiodeService: VilkårperiodeService,
) {
    /**
     * Felles format på Vedtaksperiode inneholder ennå ikke status så mapper til felles format for å kunne validere
     * vedtaksperioder på lik måte
     */
    fun validerVedtaksperioderLæremidler(
        vedtaksperioder: List<Vedtaksperiode>,
        behandling: Saksbehandling,
        typeVedtak: TypeVedtak,
    ) {
        validerVedtaksperioder(vedtaksperioder, behandling, typeVedtak)
    }

    fun validerVedtaksperioder(
        vedtaksperioder: List<Vedtaksperiode>,
        behandling: Saksbehandling,
        typeVedtak: TypeVedtak,
    ) {
        if (typeVedtak != TypeVedtak.OPPHØR) {
            validerVedtaksperioderEksisterer(vedtaksperioder)
        }
        validerIngenOverlappMellomVedtaksperioder(vedtaksperioder)

        validerVedtaksperioderMotVilkårperioder(behandling, vedtaksperioder)
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
}
