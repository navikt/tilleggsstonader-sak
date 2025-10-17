package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.VilkårDagligReiseMapper.mapTilVilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import org.springframework.stereotype.Service

@Service
class DagligReiseVilkårService(
    private val vilkårRepository: VilkårRepository,
) {
    fun hentVilkårForBehandling(behandlingId: BehandlingId): List<VilkårDagligReise> {
        return vilkårRepository.findByBehandlingId(behandlingId).map { it.mapTilVilkårDagligReise() }
    }
}
