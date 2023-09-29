package no.nav.tilleggsstonader.sak.opplysninger.mapper

import no.nav.tilleggsstonader.sak.opplysninger.dto.Kjønn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.KjønnType

object KjønnMapper {

    fun tilKjønn(kjønn: KjønnType): Kjønn = kjønn.let { Kjønn.valueOf(it.name) }
}
