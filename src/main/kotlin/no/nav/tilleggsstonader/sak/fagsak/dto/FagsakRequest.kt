package no.nav.tilleggsstonader.sak.fagsak.dto

data class FagsakRequest(
    val personIdent: String,
    val stønadstype: no.nav.tilleggsstonader.kontrakter.felles.Stønadstype,
)
