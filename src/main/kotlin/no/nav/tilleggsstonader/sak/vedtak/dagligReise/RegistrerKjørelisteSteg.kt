package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.privatbil.ManuellRegistrering.KjørelisteManuellRegistreringService
import org.springframework.stereotype.Service

@Service
class RegistrerKjørelisteSteg(
    private val kjørelisteManuellRegistreringService: KjørelisteManuellRegistreringService,
) : BehandlingSteg<Void?> {
    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: Void?,
    ) {
        kjørelisteManuellRegistreringService.avklarKjørelisterRegistrertIBehandling(saksbehandling)
    }

    override fun stegType(): StegType = StegType.REGISTRER_KJØRELISTE
}
