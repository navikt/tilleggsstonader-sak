package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.tilleggsstonader.libs.unleash.ToggleId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle

sealed interface RoutingStrategi

data object SkalRouteAlleSøkereTilNyLøsning : RoutingStrategi

data class SkalRouteEnkelteSøkereTilNyLøsning(
    val featureToggleMaksAntall: ToggleId,
    val kreverAtSøkerErUtenAktivtVedtakIArena: Boolean,
    val kreverAktivtAapVedtak: Boolean,
) : RoutingStrategi

fun bestemRoutingStrategi(søknadstype: Søknadstype): RoutingStrategi =
    when (søknadstype) {
        Søknadstype.BARNETILSYN -> SkalRouteAlleSøkereTilNyLøsning
        Søknadstype.LÆREMIDLER -> SkalRouteAlleSøkereTilNyLøsning
        Søknadstype.BOUTGIFTER -> SkalRouteAlleSøkereTilNyLøsning
        Søknadstype.DAGLIG_REISE ->
            SkalRouteEnkelteSøkereTilNyLøsning(
                featureToggleMaksAntall = Toggle.SØKNAD_ROUTING_DAGLIG_REISE,
                kreverAtSøkerErUtenAktivtVedtakIArena = true,
                kreverAktivtAapVedtak = true,
            )
    }
