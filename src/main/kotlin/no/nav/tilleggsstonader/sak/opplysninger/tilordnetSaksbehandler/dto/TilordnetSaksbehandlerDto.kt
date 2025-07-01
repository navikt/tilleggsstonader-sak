package no.nav.tilleggsstonader.sak.opplysninger.tilordnetSaksbehandler.dto

import no.nav.tilleggsstonader.sak.opplysninger.tilordnetSaksbehandler.domain.TilordnetSaksbehandler
import no.nav.tilleggsstonader.sak.opplysninger.tilordnetSaksbehandler.domain.TilordnetSaksbehandlerPåOppgave
import kotlin.String

data class TilordnetSaksbehandlerDto(
    val navIdent: String?,
    val fornavn: String?,
    val etternavn: String?,
    val tilordnetSaksbehandlerPåOppgave: TilordnetSaksbehandlerPåOppgave,
)

fun TilordnetSaksbehandler.tilDto(): TilordnetSaksbehandlerDto =
    TilordnetSaksbehandlerDto(
        navIdent = navIdent,
        fornavn = fornavn,
        etternavn = etternavn,
        tilordnetSaksbehandlerPåOppgave = tilordnetSaksbehandlerPåOppgave,
    )
