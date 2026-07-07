package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørelisteService
import org.springframework.stereotype.Service

@Service
class RegistrerKjørelisteSteg(
    private val avklartKjørelisteService: AvklartKjørelisteService,
) : BehandlingSteg<Void?> {
    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: Void?,
    ) {
        avklartKjørelisteService.avklarUkerFraRegistrerteDager(saksbehandling.id)
    }

    override fun stegType(): StegType = StegType.REGISTRER_KJØRELISTE
}
