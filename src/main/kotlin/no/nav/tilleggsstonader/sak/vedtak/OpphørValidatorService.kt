package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.springframework.stereotype.Service

@Service
class OpphørValidatorService(
    private val vilkårsperiodeService: VilkårperiodeService,
    private val vilkårService: VilkårService,
    private val vilkårperiodeRepository: VilkårperiodeRepository,
) {

    fun validerOpphør(saksbehandling_id: BehandlingId) {
        val vilkår = vilkårService.hentVilkår(saksbehandling_id)
        val vilkårperioder = vilkårsperiodeService.hentVilkårperioder(saksbehandling_id)

        validerIngenOppfylteVilkårEllerVilkårperioderMedStatusNy(vilkår, vilkårperioder)

        validerVilkår()
        validerVilkårperiode()
        validerOverlappsperiode()
    }


    private fun validerIngenOppfylteVilkårEllerVilkårperioderMedStatusNy(vilkår: List<Vilkår>, vilkårperioder: Vilkårperioder){
        val finnesOppfyltVilkårEllerVilkårperioderMedStatusNy = vilkår.any { it.status == VilkårStatus.NY && it.resultat == Vilkårsresultat.OPPFYLT } &&
                vilkårperioder.målgrupper.any{ it.status == Vilkårstatus.NY && it.resultat == ResultatVilkårperiode.OPPFYLT } &&
                vilkårperioder.aktiviteter.any{ it.status == Vilkårstatus.NY && it.resultat == ResultatVilkårperiode.OPPFYLT }

        brukerfeilHvis(finnesOppfyltVilkårEllerVilkårperioderMedStatusNy) { "Det er nye vilkår eller vilkårperiode med status NY" }
    }

    private fun validerVilkår() {
        // Resultat kan endre seg fra oppfylt til ikke oppfylt, men ikke motsatt
        // At TOM er lik eller "forkortet"
        // Har de samme fom og beløp som tidligere
        val vilkår = vilkårperiodeRepository


        TODO()
    }

    private fun validerVilkårperiode() {
        // Resultat kan endre seg fra oppfylt til ikke oppfylt
        // At TOM er lik eller "forkortet"

        //val vilkårsperioder = vilkårperiodeRepository.findByBehandlingId(behandlingId).sorted()

        TODO()
    }

    private fun validerOverlappsperiode() {
        // At TOM er lik eller "forkortet"
        TODO()
    }
}
