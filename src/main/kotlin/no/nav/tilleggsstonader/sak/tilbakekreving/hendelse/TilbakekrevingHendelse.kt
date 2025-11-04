package no.nav.tilleggsstonader.sak.tilbakekreving.hendelse

sealed interface TilbakekrevingHendelse {
    val hendelsestype: String
    val versjon: Int
}
