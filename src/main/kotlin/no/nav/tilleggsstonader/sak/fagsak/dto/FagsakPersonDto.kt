package no.nav.tilleggsstonader.sak.fagsak.dto

import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId

class FagsakPersonDto(
    val id: FagsakPersonId,
    // TODO TilsynBarn bør være PassAvBarn, men dette brukes eksternt
    val tilsynBarn: FagsakId?,
    val læremidler: FagsakId?,
    val boutgifter: FagsakId?,
    val dagligReiseTso: FagsakId?,
    val dagligReiseTsr: FagsakId?,
)
