package no.nav.tilleggsstonader.sak.fagsak.dto

import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId

class FagsakPersonDto(
    val id: FagsakPersonId,
    val tilsynBarn: FagsakId?,
    val læremidler: FagsakId?,
)

class FagsakPersonUtvidetDto(
    val id: FagsakPersonId,
    val tilsynBarn: FagsakDto?,
    val læremidler: FagsakDto?,
)
