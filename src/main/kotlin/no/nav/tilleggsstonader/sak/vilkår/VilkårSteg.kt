package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import org.springframework.stereotype.Service

@Service
class VilkårSteg : BehandlingSteg<Void?> {
    override fun utførSteg(saksbehandling: Saksbehandling, data: Void?) {}

    override fun stegType(): StegType = StegType.VILKÅR
}
