package no.nav.tilleggsstonader.sak.vedtak.validering

import no.nav.tilleggsstonader.libs.feil.feilHvis
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringUtils.validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringUtils.validerEnkeltperiode
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringUtils.validerIngenOverlappMellomVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringUtils.validerVedtaksperioderEksisterer
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.mergeSammenhengendeOppfylteAktiviteter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.mergeSammenhengendeOppfylteMålgrupper
import org.springframework.stereotype.Service

@Service
class VedtaksperiodeValideringService(
    private val vilkårperiodeService: VilkårperiodeService,
    private val unleashService: UnleashService,
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
        // Validerer målgrupper aktivitetspenger og ungdomsprogrammet mot feature-toggle
        val målgrupper = vilkårperiodeService.hentVilkårperioder(behandling.id).målgrupper
        vedtaksperioder.forEach { vedtaksperiode ->
            målgrupper
                .filter { målgruppe ->
                    val målgruppeType = målgruppe.type as? MålgruppeType
                    målgruppeType?.faktiskMålgruppeEllerNull() == vedtaksperiode.målgruppe &&
                        målgruppe.fom <= vedtaksperiode.tom &&
                        målgruppe.tom >= vedtaksperiode.fom
                }.forEach { validerFeatureToggle(it.type) }
        }

        if (typeVedtak != TypeVedtak.OPPHØR) {
            validerVedtaksperioderEksisterer(vedtaksperioder)
        }

        validerIngenOverlappMellomVedtaksperioder(vedtaksperioder)

        validerVedtaksperioderMotVilkårperioder(behandling, vedtaksperioder)
    }

    private fun validerFeatureToggle(målgruppe: VilkårperiodeType) {
        if (målgruppe == MålgruppeType.UNGDOMSPROGRAMMET) {
            feilHvis(!unleashService.isEnabled(Toggle.KAN_BRUKE_MÅLGRUPPE_UNGDOMSPROGRAMMET)) {
                "Ungdomsprogrammet er ikke aktivert"
            }
        } else if (målgruppe == MålgruppeType.AKTIVITETSPENGER) {
            feilHvis(!unleashService.isEnabled(Toggle.KAN_BRUKE_MÅLGRUPPE_AKTIVITETSPENGER)) {
                "Aktivitetspenger er ikke aktivert"
            }
        }
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
