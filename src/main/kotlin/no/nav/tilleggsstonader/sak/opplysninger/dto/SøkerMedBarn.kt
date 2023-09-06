package no.nav.tilleggsstonader.sak.opplysninger.dto

import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlPersonForelderBarn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlSøker

data class SøkerMedBarn(
    val søkerIdent: String,
    val søker: PdlSøker,
    val barn: Map<String, PdlPersonForelderBarn>,
)
