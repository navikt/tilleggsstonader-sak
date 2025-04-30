package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.UtgiftBeregningMåned
import no.nav.tilleggsstonader.sak.vedtak.tilUtgiftBeregning
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import org.springframework.stereotype.Service

@Service
class TilsynBarnUtgiftService(
    private val vilkårService: VilkårService,
) {
    fun hentUtgifterTilBeregning(behandlingId: BehandlingId): Map<BarnId, List<UtgiftBeregningMåned>> =
        vilkårService
            .hentOppfyltePassBarnVilkår(behandlingId)
            .groupBy { it.barnId ?: error("Vilkår=${it.id} type=${it.type} for tilsyn barn mangler barnId") }
            .mapValues { (_, values) -> values.map { it.tilUtgiftBeregning() as UtgiftBeregningMåned } }
}
