package no.nav.tilleggsstonader.sak.tilbakekreving.domene

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import org.springframework.data.annotation.Id

data class TilbakekrevingHendelse(
    @Id
    val id: Long = 0,
    val behandlingId: BehandlingId,
    val hendelse: TilbakekrevingJson,
)
