package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import org.springframework.stereotype.Service

@Service
class InngangsvilkårSteg(
    private val behandlingService: BehandlingService,
    private val stønadsperiodeService: StønadsperiodeService,
    private val unleashService: UnleashService,
) : BehandlingSteg<Void?> {
    override fun validerSteg(saksbehandling: Saksbehandling) {
        if (!unleashService.isEnabled(Toggle.KAN_BRUKE_VEDTAKSPERIODER_TILSYN_BARN)) {
            stønadsperiodeService.validerStønadsperioder(saksbehandling.id)
        }
        brukerfeilHvis(saksbehandling.type == BehandlingType.REVURDERING && saksbehandling.revurderFra == null) {
            "Du må sette revurder fra-dato før du kan gå videre"
        }
    }

    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: Void?,
    ) {
        if (saksbehandling.status != BehandlingStatus.UTREDES) {
            behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, BehandlingStatus.UTREDES)
        }
    }

    /**
     * håndteres av [StønadsperiodeService]
     */
    override fun settInnHistorikk() = false

    override fun stegType(): StegType = StegType.INNGANGSVILKÅR
}
