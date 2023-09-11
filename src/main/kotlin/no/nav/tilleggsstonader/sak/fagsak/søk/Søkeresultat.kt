package no.nav.tilleggsstonader.sak.fagsak.søk

import no.nav.tilleggsstonader.sak.opplysninger.dto.Kjønn
import java.util.UUID

data class Søkeresultat(
    val personIdent: String,
    val visningsnavn: String,
    val kjønn: Kjønn,
    val fagsakPersonId: UUID?,
)

data class SøkeresultatUtenFagsak(val personIdent: String, val navn: String)
