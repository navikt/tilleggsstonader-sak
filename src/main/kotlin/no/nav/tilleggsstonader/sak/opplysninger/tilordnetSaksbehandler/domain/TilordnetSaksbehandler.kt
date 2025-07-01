package no.nav.tilleggsstonader.sak.opplysninger.tilordnetSaksbehandler.domain

data class TilordnetSaksbehandler(
    val navIdent: String?,
    val fornavn: String?,
    val etternavn: String?,
    val tilordnetSaksbehandlerPåOppgave: TilordnetSaksbehandlerPåOppgave,
)

enum class TilordnetSaksbehandlerPåOppgave {
    IKKE_SATT,
    INNLOGGET_SAKSBEHANDLER,
    ANNEN_SAKSBEHANDLER,
    OPPGAVE_FINNES_IKKE,
    OPPGAVE_FINNES_IKKE_SANNSYNLIGVIS_INNLOGGET_SAKSBEHANDLER,
}
