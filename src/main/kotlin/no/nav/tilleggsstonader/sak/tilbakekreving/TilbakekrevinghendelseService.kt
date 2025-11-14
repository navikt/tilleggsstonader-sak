package no.nav.tilleggsstonader.sak.tilbakekreving

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilbakekreving.domene.Tilbakekrevinghendelse
import no.nav.tilleggsstonader.sak.tilbakekreving.domene.Tilbakekrevingsstatus
import org.springframework.stereotype.Service

@Service
class TilbakekrevinghendelseService(
    private val tilbakekrevinghendelseRepository: TilbakekrevinghendelseRepository,
) {
    fun persisterHendelse(
        behandlingId: BehandlingId,
        tilbakekrevingsstatus: Tilbakekrevingsstatus,
    ) {
        tilbakekrevinghendelseRepository.insert(
            Tilbakekrevinghendelse(
                behandlingId = behandlingId,
                hendelse = tilbakekrevingsstatus,
            ),
        )
    }

    fun hentHendelserForBehandling(behandlingId: BehandlingId): List<Tilbakekrevinghendelse> =
        tilbakekrevinghendelseRepository.findAllByBehandlingId(behandlingId)
}
