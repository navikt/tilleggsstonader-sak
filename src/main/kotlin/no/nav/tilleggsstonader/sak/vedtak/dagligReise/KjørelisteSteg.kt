package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import org.springframework.stereotype.Service

@Service
class KjørelisteSteg : BehandlingSteg<Void?> {
    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: Void?,
    ) {
        // TODO: Beregn kjøreliste og lagre ned resultat
    }

    override fun stegType(): StegType = StegType.KJØRELISTE
}
