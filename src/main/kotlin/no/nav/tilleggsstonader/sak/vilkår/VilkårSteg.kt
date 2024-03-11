package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.OppdaterVilkår
import org.springframework.stereotype.Service

@Service
class VilkårSteg(
    private val behandlingService: BehandlingService,
    private val vilkårService: VilkårService,
) : BehandlingSteg<Void?> {
    override fun utførSteg(saksbehandling: Saksbehandling, data: Void?) {
        if (saksbehandling.status != BehandlingStatus.UTREDES) {
            behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, BehandlingStatus.UTREDES)
        }
    }

    override fun validerSteg(saksbehandling: Saksbehandling) {
        val vilkårsresultat = vilkårService.hentVilkårsresultat(saksbehandling.id)

        brukerfeilHvisIkke(OppdaterVilkår.erAlleVilkårTattStillingTil(vilkårsresultat)){
            "Alle vilkår må være tatt stilling til"
        }
    }

    override fun stegType(): StegType = StegType.VILKÅR

    /**
     * håndteres av [VilkårStegService]
     */
    override fun settInnHistorikk(): Boolean = false
}
