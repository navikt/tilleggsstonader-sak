package no.nav.tilleggsstonader.sak.tilbakekreving

import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.tilbakekreving.domene.TilbakekrevingHendelse
import no.nav.tilleggsstonader.sak.tilbakekreving.domene.Tilbakekrevingsstatus
import no.nav.tilleggsstonader.sak.tilbakekreving.dto.TilbakekrevingHendelseDto
import no.nav.tilleggsstonader.sak.tilbakekreving.dto.tilDto
import org.springframework.stereotype.Service

@Service
class TilbakekrevinghendelseService(
    private val tilbakekrevinghendelseRepository: TilbakekrevinghendelseRepository,
    private val unleashService: UnleashService,
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

    fun harHendelserForBehandling(behandlingId: BehandlingId): Boolean =
        unleashService.isEnabled(Toggle.KAN_VISE_TILBAKEKREVING) &&
            tilbakekrevinghendelseRepository.existsByBehandlingId(behandlingId)

    fun hentBehandlingEndretHendelser(behandlingId: BehandlingId): List<TilbakekrevingHendelseDto> =
        hentHendelserForBehandling(behandlingId)
            .map { it.hendelse }
            .filterIsInstance<Tilbakekrevingsstatus>()
            .sortedBy { it.hendelseOpprettet }
            .map { it.tilDto() }

    fun harMottattHendelseMedStatus(
        behandlingId: BehandlingId,
        behandlingstatus: String,
    ): Boolean =
        hentHendelserForBehandling(behandlingId)
            .map { it.hendelse }
            .filterIsInstance<Tilbakekrevingsstatus>()
            .any { it.behandlingstatus == behandlingstatus }
}
