package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.tilleggsstonader.kontrakter.felles.Søknadstype

data class SøknadRoutingDto(
    val ident: String,
    val søknadstype: Søknadstype,
)
