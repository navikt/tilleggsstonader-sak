package no.nav.tilleggsstonader.sak.tilbakekreving

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilbakekreving.domene.TilbakekrevingHendelse
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
            TilbakekrevingHendelse(
                behandlingId = behandlingId,
                hendelse = tilbakekrevingsstatus,
            ),
        )
    }

    fun hentHendelserForBehandling(behandlingId: BehandlingId): List<TilbakekrevingHendelse> =
        tilbakekrevinghendelseRepository.findAllByBehandlingId(behandlingId)
}
