package no.nav.tilleggsstonader.sak.behandling.dto

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype

data class HentBehandlingerRequest(
    val personIdent: String,
    val stønadstype: Stønadstype,
)