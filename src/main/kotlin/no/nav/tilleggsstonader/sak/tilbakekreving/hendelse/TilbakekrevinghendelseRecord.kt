package no.nav.tilleggsstonader.sak.tilbakekreving.hendelse

sealed interface TilbakekrevinghendelseRecord {
    val hendelsestype: String
    val versjon: Int
    val eksternFagsakId: String
}
