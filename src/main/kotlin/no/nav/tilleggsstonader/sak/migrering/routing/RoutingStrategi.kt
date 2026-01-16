package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.tilleggsstonader.kontrakter.felles.Skjematype
import no.nav.tilleggsstonader.libs.unleash.ToggleId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle

sealed interface RoutingStrategi {
    data object SendAlleBrukereTilNyLøsning : RoutingStrategi

    data class SendEnkelteBrukereTilNyLøsning(
        val featureToggleMaksAntallForStønad: ToggleId,
        val kreverAtSøkerErUtenAktivtVedtakIArena: Boolean,
        val skalBegrenseAntallTilTsr: Boolean,
        val featureToggleMaksAntallForTsr: Toggle,
        val kreverAktivtAapVedtak: Boolean,
        val kreverUgradertAdresse: Boolean,
    ) : RoutingStrategi
}

fun bestemRoutingStrategi(skjematype: Skjematype): RoutingStrategi =
    when (skjematype) {
        Skjematype.SØKNAD_BARNETILSYN -> RoutingStrategi.SendAlleBrukereTilNyLøsning
        Skjematype.SØKNAD_LÆREMIDLER -> RoutingStrategi.SendAlleBrukereTilNyLøsning
        Skjematype.SØKNAD_BOUTGIFTER -> RoutingStrategi.SendAlleBrukereTilNyLøsning
        Skjematype.SØKNAD_DAGLIG_REISE ->
            RoutingStrategi.SendEnkelteBrukereTilNyLøsning(
                featureToggleMaksAntallForStønad = Toggle.SØKNAD_ROUTING_DAGLIG_REISE,
                kreverAtSøkerErUtenAktivtVedtakIArena = true,
                featureToggleMaksAntallForTsr = Toggle.SØKNAD_ROUTING_DAGLIG_REISE_TSR,
                skalBegrenseAntallTilTsr = true,
                kreverAktivtAapVedtak = true,
                kreverUgradertAdresse = true,
            )

        Skjematype.DAGLIG_REISE_KJØRELISTE -> TODO()
    }
