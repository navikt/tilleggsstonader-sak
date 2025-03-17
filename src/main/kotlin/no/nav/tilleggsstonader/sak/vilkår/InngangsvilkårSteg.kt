package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import org.springframework.stereotype.Service

@Service
class InngangsvilkårSteg(
    private val behandlingService: BehandlingService,
    private val stønadsperiodeService: StønadsperiodeService,
) : BehandlingSteg<Void?> {
    override fun validerSteg(saksbehandling: Saksbehandling) {
        if (saksbehandling.stønadstype == Stønadstype.LÆREMIDLER) {
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
