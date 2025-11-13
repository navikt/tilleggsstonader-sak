package no.nav.tilleggsstonader.sak.tilbakekreving.håndter

import com.fasterxml.jackson.databind.JsonNode
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import org.springframework.stereotype.Service

@Service
class BehandlingEndretHåndterer(
    private val oppgaveService: OppgaveService,
) : TilbakekrevingHendelseHåndterer {
    override fun håndtererHendelsetype(): String = "behandling_endret"

    override fun håndter(
        hendelseKey: String,
        payload: JsonNode,
    ) {
        oppgaveService.opprettTilbakekrevingsoppgave()
    }
}
