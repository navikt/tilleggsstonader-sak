package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.tilleggsstonader.libs.unleash.ToggleId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle

sealed interface RoutingContext {
    val ident: String
    val søknadstype: Søknadstype
}

data class SkalRouteAlleSøkereTilNyLøsning(
    override val ident: String,
    override val søknadstype: Søknadstype,
) : RoutingContext {
    companion object {
        fun fraSøknadRoutingDto(identStønadstype: SøknadRoutingDto) =
            SkalRouteAlleSøkereTilNyLøsning(
                ident = identStønadstype.ident,
                søknadstype = identStønadstype.søknadstype,
            )
    }
}

data class SkalRouteEnkelteSøkereTilNyLøsning(
    override val ident: String,
    override val søknadstype: Søknadstype,
    val toggleId: ToggleId,
) : RoutingContext

fun bestemRoutingStrategi(routingRequest: SøknadRoutingDto) =
    when (routingRequest.søknadstype) {
        Søknadstype.BARNETILSYN -> SkalRouteAlleSøkereTilNyLøsning.fraSøknadRoutingDto(routingRequest)
        Søknadstype.LÆREMIDLER -> SkalRouteAlleSøkereTilNyLøsning.fraSøknadRoutingDto(routingRequest)
        Søknadstype.BOUTGIFTER -> SkalRouteAlleSøkereTilNyLøsning.fraSøknadRoutingDto(routingRequest)
        Søknadstype.DAGLIG_REISE ->
            SkalRouteEnkelteSøkereTilNyLøsning(
                ident = routingRequest.ident,
                søknadstype = routingRequest.søknadstype,
                toggleId = Toggle.SØKNAD_ROUTING_DAGLIG_REISE,
            )
    }
