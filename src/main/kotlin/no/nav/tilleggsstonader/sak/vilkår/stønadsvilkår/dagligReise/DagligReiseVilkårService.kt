package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.VilkårDagligReiseMapper.mapTilVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.VilkårDagligReiseMapper.mapTilVilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.LagreDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.RegelEvaluering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping.ByggVilkårFraSvar
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.DagligReiseOffentiligTransportRegel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DagligReiseVilkårService(
    private val vilkårRepository: VilkårRepository,
) {
    fun hentVilkårForBehandling(behandlingId: BehandlingId): List<VilkårDagligReise> =
        vilkårRepository.findByBehandlingId(behandlingId).map { it.mapTilVilkårDagligReise() }

    @Transactional
    fun opprettNyttVilkår(nyttVilkår: LagreDagligReise): VilkårDagligReise {
        val delvilkårsett =
            ByggVilkårFraSvar.byggDelvilkårsettFraSvarOgVilkårsregel(
                vilkårsregel = DagligReiseOffentiligTransportRegel(),
                svar = nyttVilkår.svar,
            )

        val vilkår =
            VilkårDagligReise(
                behandlingId = nyttVilkår.behandlingId,
                fom = nyttVilkår.fom,
                tom = nyttVilkår.tom,
                status = VilkårStatus.NY,
                delvilkårsett = delvilkårsett,
                resultat = RegelEvaluering.utledVilkårResultat(delvilkårsett),
                fakta = nyttVilkår.fakta?.mapTilFakta(),
            )

        val lagretVilkår = vilkårRepository.insert(vilkår.mapTilVilkår())

        return lagretVilkår.mapTilVilkårDagligReise()
    }
}
