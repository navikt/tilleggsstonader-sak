package no.nav.tilleggsstonader.sak.migrering.routing

enum class SkjemaRoutingAksjon {
    NY_LØSNING,
    GAMMEL_LØSNING,
    AVSJEKK,
}

data class SøknadRoutingResponse(
    val aksjon: SkjemaRoutingAksjon,
)
