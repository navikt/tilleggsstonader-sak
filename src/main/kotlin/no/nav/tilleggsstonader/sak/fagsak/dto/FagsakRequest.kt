package no.nav.tilleggsstonader.sak.fagsak.dto

import no.nav.tilleggsstonader.sak.fagsak.Stønadstype

data class FagsakRequest(
    val personIdent: String,
    val stønadstype: Stønadstype,
)
