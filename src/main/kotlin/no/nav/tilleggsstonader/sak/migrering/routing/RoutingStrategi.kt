package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.tilleggsstonader.kontrakter.felles.Skjematype
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

fun bestemRoutingStrategi(skjematype: Skjematype): RoutingStrategi =
    when (skjematype) {
        Skjematype.BARNETILSYN -> RoutingStrategi.RouteAlleSøkereTilNyLøsning
        Skjematype.LÆREMIDLER -> RoutingStrategi.RouteAlleSøkereTilNyLøsning
        Skjematype.BOUTGIFTER -> RoutingStrategi.RouteAlleSøkereTilNyLøsning
        Skjematype.DAGLIG_REISE ->
            RoutingStrategi.RouteEnkelteSøkereTilNyLøsning(
                featureToggleMaksAntall = Toggle.SØKNAD_ROUTING_DAGLIG_REISE,
                kreverAtSøkerErUtenAktivtVedtakIArena = true,
                kreverAktivtAapVedtak = true,
            )
        Skjematype.DAGLIG_REISE_KJØRELISTE -> TODO()
    }
