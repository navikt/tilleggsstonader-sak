package no.nav.tilleggsstonader.sak.infrastruktur.unleash

import no.nav.tilleggsstonader.libs.unleash.ToggleId

enum class Toggle(override val toggleId: String) : ToggleId {
    SØKNAD_ROUTING_TILSYN_BARN("sak.soknad-routing.tilsyn-barn"),
    KAN_OPPRETTE_BEHANDLING("sak.kan-opprette-behandling"),
}
