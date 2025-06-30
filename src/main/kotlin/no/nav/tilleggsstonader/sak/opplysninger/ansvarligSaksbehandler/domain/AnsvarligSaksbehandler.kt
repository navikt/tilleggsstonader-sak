package no.nav.tilleggsstonader.sak.opplysninger.ansvarligSaksbehandler.domain

import no.nav.tilleggsstonader.sak.opplysninger.ansvarligSaksbehandler.dto.AnsvarligSaksbehandlerDto

data class AnsvarligSaksbehandler(
    val fornavn: String?,
    val etternavn: String?,
    val rolle: SaksbehandlerRolle,
)

enum class SaksbehandlerRolle {
    IKKE_SATT,
    INNLOGGET_SAKSBEHANDLER,
    ANNEN_SAKSBEHANDLER,
    OPPGAVE_FINNES_IKKE,
    OPPGAVE_FINNES_IKKE_SANNSYNLIGVIS_INNLOGGET_SAKSBEHANDLER,
}

fun AnsvarligSaksbehandler.tilDto(): AnsvarligSaksbehandlerDto =
    AnsvarligSaksbehandlerDto(
        fornavn = fornavn,
        etternavn = etternavn,
        rolle = rolle,
    )
