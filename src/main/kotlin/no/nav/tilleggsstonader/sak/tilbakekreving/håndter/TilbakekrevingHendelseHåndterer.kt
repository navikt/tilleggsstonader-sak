package no.nav.tilleggsstonader.sak.tilbakekreving.håndter

import com.fasterxml.jackson.databind.JsonNode
import no.nav.tilleggsstonader.sak.tilbakekreving.hendelse.TilbakekrevinghendelseRecord

sealed interface TilbakekrevingHendelseHåndterer {
    fun håndtererHendelsetype(): String

    fun håndter(
        hendelseKey: String,
        payload: JsonNode,
    )

    fun gjelderTestsak(tilbakekrevinghendelseRecord: TilbakekrevinghendelseRecord) =
        !tilbakekrevinghendelseRecord.eksternFagsakId.all { it.isDigit() }
}
