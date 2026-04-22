package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.tilleggsstonader.kontrakter.felles.Skjematype
import no.nav.tilleggsstonader.libs.unleash.ToggleId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle

sealed interface RoutingStrategi {
    data object SendAlleBrukereTilNyLøsning : RoutingStrategi

    data class SendEnkelteBrukereTilNyLøsning(
        val featureToggleMaksAntallForStønad: ToggleId,
        val kreverAtSøkerErUtenAktivtVedtakIArena: Boolean,
        val kreverAktivtAapVedtak: Boolean,
        val kreverUgradertAdresse: Boolean,
        val alleMedAAPVedtakTilNyLøsning: Boolean,
    ) : RoutingStrategi

    data object KjørelisteRouting : RoutingStrategi
}

fun bestemRoutingStrategi(skjematype: Skjematype): RoutingStrategi =
    when (skjematype) {
        Skjematype.SØKNAD_BARNETILSYN -> RoutingStrategi.SendAlleBrukereTilNyLøsning
        Skjematype.SØKNAD_LÆREMIDLER -> RoutingStrategi.SendAlleBrukereTilNyLøsning
        Skjematype.SØKNAD_BOUTGIFTER -> RoutingStrategi.SendAlleBrukereTilNyLøsning
        Skjematype.SØKNAD_DAGLIG_REISE ->
            RoutingStrategi.SendEnkelteBrukereTilNyLøsning(
                featureToggleMaksAntallForStønad = Toggle.SØKNAD_ROUTING_PRIVAT_BIL,
                kreverAtSøkerErUtenAktivtVedtakIArena = true,
                kreverAktivtAapVedtak = false,
                kreverUgradertAdresse = true,
                alleMedAAPVedtakTilNyLøsning = true,
            )

        Skjematype.DAGLIG_REISE_KJØRELISTE -> RoutingStrategi.KjørelisteRouting
        Skjematype.SØKNAD_REISE_TIL_SAMLING ->
            RoutingStrategi.SendEnkelteBrukereTilNyLøsning(
                featureToggleMaksAntallForStønad = Toggle.SØKNAD_ROUTING_REISE_TIL_SAMLING,
                kreverAtSøkerErUtenAktivtVedtakIArena = true,
                kreverAktivtAapVedtak = false,
                kreverUgradertAdresse = true,
                alleMedAAPVedtakTilNyLøsning = false,
            )
    }
