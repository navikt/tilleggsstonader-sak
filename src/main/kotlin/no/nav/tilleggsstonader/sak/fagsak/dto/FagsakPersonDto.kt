package no.nav.tilleggsstonader.sak.fagsak.dto

import java.util.UUID

class FagsakPersonDto(
    val id: UUID,
    val barnetilsyn: UUID?,
)

class FagsakPersonUtvidetDto(
    val id: UUID,
    val barnetilsyn: FagsakDto?,
)
