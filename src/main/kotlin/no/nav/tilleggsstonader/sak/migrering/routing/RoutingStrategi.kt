package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.tilleggsstonader.libs.unleash.ToggleId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle

sealed interface RoutingStrategi {
    data object RouteAlleSøkereTilNyLøsning : RoutingStrategi

    data class RouteEnkelteSøkereTilNyLøsning(
        val featureToggleMaksAntall: ToggleId,
        val kreverAtSøkerErUtenAktivtVedtakIArena: Boolean,
        val kreverAktivtAapVedtak: Boolean,
    ) : RoutingStrategi
}

fun bestemRoutingStrategi(søknadstype: Søknadstype): RoutingStrategi =
    when (søknadstype) {
        Søknadstype.BARNETILSYN -> RoutingStrategi.RouteAlleSøkereTilNyLøsning
        Søknadstype.LÆREMIDLER -> RoutingStrategi.RouteAlleSøkereTilNyLøsning
        Søknadstype.BOUTGIFTER -> RoutingStrategi.RouteAlleSøkereTilNyLøsning
        Søknadstype.DAGLIG_REISE ->
            RoutingStrategi.RouteEnkelteSøkereTilNyLøsning(
                featureToggleMaksAntall = Toggle.SØKNAD_ROUTING_DAGLIG_REISE,
                kreverAtSøkerErUtenAktivtVedtakIArena = true,
                kreverAktivtAapVedtak = true,
            )
    }
