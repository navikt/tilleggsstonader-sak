package no.nav.tilleggsstonader.sak.fagsak.søk

import java.util.UUID

data class Søkeresultat(
    val personIdent: String,
    val visningsnavn: String,
    val fagsakPersonId: UUID?,
)

data class SøkeresultatUtenFagsak(val personIdent: String, val navn: String)
