package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.springframework.stereotype.Service

@Service
class OpphørValidatorService(
    private val vilkårsperiodeService: VilkårperiodeService,
    private val vilkårService: VilkårService,
) {

    fun validerOpphør(saksbehandling_id: BehandlingId) {
        val vilkår = vilkårService.hentVilkår(saksbehandling_id)
        val vilkårperioder = vilkårsperiodeService.hentVilkårperioder(saksbehandling_id)

        val ingenMedStatusNy = validerIngenVilkårEllerVilkårperiodeMedStatusNy(vilkår, vilkårperioder)

        validerVilkår()
        validerVilkårperiode()
        validerOverlappsperiode()
        brukerfeilHvisIkke(ingenMedStatusNy) { "Det er vilkår eller vilkårperiode med status NY" }
    }

    private fun validerIngenVilkårEllerVilkårperiodeMedStatusNy(vilkår: List<Vilkår>, vilkårperioder: Vilkårperioder) =
        vilkår.none { it.status == VilkårStatus.NY } &&
            vilkårperioder.målgrupper.none { it.status == Vilkårstatus.NY } &&
            vilkårperioder.aktiviteter.none { it.status == Vilkårstatus.NY }

    private fun validerVilkår() {
        // Resultat kan endre seg fra oppfylt til ikke oppfylt
        // At TOM er lik eller "forkortet"
        // Har de samme fom og beløp som tidligere
        TODO()
    }

    private fun validerVilkårperiode() {
        // Resultat kan endre seg fra oppfylt til ikke oppfylt
        // At TOM er lik eller "forkortet"
        TODO()
    }

    private fun validerOverlappsperiode() {
        // At TOM er lik eller "forkortet"
        TODO()
    }
}
