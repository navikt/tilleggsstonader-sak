package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.VilkårPeriodeValidering.validerIkkeOverlappendeVilkår
import org.springframework.stereotype.Service

@Service
class VilkårSteg(
    private val behandlingService: BehandlingService,
    private val vilkårService: VilkårService,
) : BehandlingSteg<Void?> {
    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: Void?,
    ) {
        if (saksbehandling.status != BehandlingStatus.UTREDES) {
            behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, BehandlingStatus.UTREDES)
        }
    }

    override fun validerSteg(saksbehandling: Saksbehandling) {
        val vilkår = vilkårService.hentVilkår(saksbehandling.id)

        validerIkkeOverlappendeVilkår(vilkår)

        val manglerVilkår = vilkår.isEmpty()
        brukerfeilHvis(manglerVilkår) {
            "Mangler vilkår, vennligst legg til et vilkår for reiseperioden."
        }

        val manglerVerdierPåOppfylteVilkår =
            vilkår
                .filter { it.resultat == Vilkårsresultat.OPPFYLT }
                .any { it.fom == null || it.tom == null || (manglerPåkrevdUtgift(it)) }
        brukerfeilHvis(manglerVerdierPåOppfylteVilkår) {
            "Mangler fom, tom eller utgift på et eller flere vilkår. " +
                "Vennligst ta stilling til hvilken periode vilkåret gjelder for."
        }
        val manglerVerdierPåIkkeOppfylteVilkår =
            vilkår
                .filter { it.resultat == Vilkårsresultat.IKKE_OPPFYLT }
                .any { it.fom == null || it.tom == null }
        brukerfeilHvis(manglerVerdierPåIkkeOppfylteVilkår) {
            "Mangler fom eller tom på et eller flere vilkår. " +
                "Vennligst ta stilling til hvilken periode vilkåret gjelder for."
        }
        val negativeBillettpriser =
            vilkår
                .filter { it.resultat == Vilkårsresultat.OPPFYLT }
                .any {
                    it.offentligTransport?.prisEnkelbillett!! < 0 || it.offentligTransport.prisSyvdagersbillett!! < 0 ||
                        it.offentligTransport.prisTrettidagersbillett < 0
                }
        brukerfeilHvis(negativeBillettpriser) {
            "Det er oppgitt en ugyldig billettpris. Beløpet kan ikke være negativt."
        }
        val negativeReisedager =
            vilkår
                .filter { it.resultat == Vilkårsresultat.OPPFYLT }
                .any { it.offentligTransport?.reisedagerPerUke!! < 0 }
        brukerfeilHvis(negativeReisedager) {
            "Det er oppgitt et ugyldig antall reisedager per uke. Verdien kan ikke være negativ."
        }
    }

    override fun stegType(): StegType = StegType.VILKÅR

    private fun manglerPåkrevdUtgift(vilkår: Vilkår): Boolean =
        !vilkår.erFremtidigUtgift &&
            harIkkeOffentligTransport(vilkår) &&
            vilkår.utgift == null

    private fun harIkkeOffentligTransport(vilkår: Vilkår) = vilkår.offentligTransport == null
}
