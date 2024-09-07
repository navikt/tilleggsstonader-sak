package no.nav.tilleggsstonader.sak.fagsak.dto

import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import java.util.UUID

class FagsakPersonDto(
    val id: FagsakPersonId,
    val tilsynBarn: UUID?,
)

class FagsakPersonUtvidetDto(
    val id: FagsakPersonId,
    val tilsynBarn: FagsakDto?,
)
