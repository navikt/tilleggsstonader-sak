package no.nav.tilleggsstonader.sak.fagsak.søk

import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId

data class Søkeresultat(
    val personIdent: String,
    val visningsnavn: String,
    val fagsakPersonId: FagsakPersonId?,
)

data class SøkeresultatUtenFagsak(val personIdent: String, val navn: String)
