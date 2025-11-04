package no.nav.tilleggsstonader.sak.tilbakekreving.håndter

import com.fasterxml.jackson.databind.JsonNode

sealed interface TilbakekrevingHendelseHåndterer {
    fun håndtererHendelsetype(): String

    fun håndter(
        hendelseKey: String,
        payload: JsonNode,
    )
}
