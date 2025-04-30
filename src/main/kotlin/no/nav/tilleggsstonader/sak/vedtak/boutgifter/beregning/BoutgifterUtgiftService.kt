package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.UtgiftBeregningDato
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.tilUtgiftBeregning
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import org.springframework.stereotype.Service

@Service
class BoutgifterUtgiftService(
    private val vilkårService: VilkårService,
) {
    fun hentUtgifterTilBeregning(behandlingId: BehandlingId): Map<TypeBoutgift, List<UtgiftBeregningDato>> =
        vilkårService
            .hentOppfylteBoutgiftVilkår(behandlingId)
            .groupBy { TypeBoutgift.fraVilkårType(it.type) }
            .mapValues { (_, values) -> values.map { it.tilUtgiftBeregning() as UtgiftBeregningDato } }
}
