package no.nav.tilleggsstonader.sak.opplysninger.dto

import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlBarn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlSøker

data class SøkerMedBarn(
    val søkerIdent: String,
    val søker: PdlSøker,
    val barn: Map<String, PdlBarn>,
)
