package no.nav.tilleggsstonader.sak.opplysninger.ansvarligSaksbehandler.dto

import no.nav.tilleggsstonader.sak.opplysninger.ansvarligSaksbehandler.domain.SaksbehandlerRolle

data class AnsvarligSaksbehandlerDto(
    val fornavn: String?,
    val etternavn: String?,
    val rolle: SaksbehandlerRolle,
)
