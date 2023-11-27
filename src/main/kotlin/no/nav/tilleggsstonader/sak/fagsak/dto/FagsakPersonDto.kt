package no.nav.tilleggsstonader.sak.fagsak.dto

import java.util.UUID

class FagsakPersonDto(
    val id: UUID,
    val tilsynBarn: UUID?,
)

class FagsakPersonUtvidetDto(
    val id: UUID,
    val tilsynBarn: FagsakDto?,
)
