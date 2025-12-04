package no.nav.tilleggsstonader.sak.tilbakekreving.håndter

import no.nav.tilleggsstonader.sak.tilbakekreving.hendelse.TilbakekrevinghendelseRecord
import tools.jackson.databind.JsonNode

sealed interface TilbakekrevingHendelseHåndterer {
    fun håndtererHendelsetype(): String

    fun håndter(
        hendelseKey: String,
        payload: JsonNode,
    )

    fun gjelderTestsak(tilbakekrevinghendelseRecord: TilbakekrevinghendelseRecord) =
        !tilbakekrevinghendelseRecord.eksternFagsakId.all { it.isDigit() }
}
