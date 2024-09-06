package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.VilkårPeriodeValidering.validerIkkeOverlappendeVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.VilkårsresultatUtil
import org.springframework.stereotype.Service

@Service
class VilkårSteg(
    private val behandlingService: BehandlingService,
    private val vilkårService: VilkårService,
    private val unleashService: UnleashService,
) : BehandlingSteg<Void?> {
    override fun utførSteg(saksbehandling: Saksbehandling, data: Void?) {
        if (saksbehandling.status != BehandlingStatus.UTREDES) {
            behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, BehandlingStatus.UTREDES)
        }
    }

    override fun validerSteg(saksbehandling: Saksbehandling) {
        val vilkår = vilkårService.hentVilkår(saksbehandling.id)
        val vilkårsresultat = VilkårsresultatUtil.utledVilkårsresultat(vilkår)

        brukerfeilHvisIkke(VilkårsresultatUtil.erAlleVilkårTattStillingTil(vilkårsresultat)) {
            "Alle vilkår må være tatt stilling til"
        }
        validerIkkeOverlappendeVilkår(vilkår)

        if (unleashService.isEnabled(Toggle.VILKÅR_PERIODISERING)) {
            val manglerVerdierPåOppfylteVilkår =
                vilkår.filter { it.resultat == Vilkårsresultat.OPPFYLT }
                    .any { it.fom == null || it.tom == null || it.utgift == null }
            brukerfeilHvis(manglerVerdierPåOppfylteVilkår) {
                "Mangler fom, tom eller utgift på et eller flere vilkår. " +
                    "Vennligst ta stilling til hvilken periode vilkåret gjelder for."
            }
            val manglerVerdierPåIkkeOppfylteVilkår =
                vilkår.filter { it.resultat == Vilkårsresultat.IKKE_OPPFYLT }
                    .any { it.fom == null || it.tom == null }
            brukerfeilHvis(manglerVerdierPåIkkeOppfylteVilkår) {
                "Mangler fom eller tom på et eller flere vilkår. " +
                    "Vennligst ta stilling til hvilken periode vilkåret gjelder for."
            }
        }
    }

    override fun stegType(): StegType = StegType.VILKÅR
}
