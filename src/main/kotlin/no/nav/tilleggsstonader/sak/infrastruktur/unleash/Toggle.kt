package no.nav.tilleggsstonader.sak.infrastruktur.unleash

import no.nav.tilleggsstonader.libs.unleash.ToggleId

enum class Toggle(override val toggleId: String) : ToggleId {
    SØKNAD_ROUTING_TILSYN_BARN("sak.soknad-routing.tilsyn-barn"),

    KAN_OPPRETTE_BEHANDLING("sak.kan-opprette-behandling"),
    KAN_OPPRETTE_BEHANDLING_FRA_JOURNALPOST("sak.kan-opprette-behandling-fra-journalpost"),
    KAN_OPPRETTE_REVURDERING("sak.kan-opprette-revurdering"),

    AUTOMATISK_JOURNALFORING_REVURDERING("sak.automatisk-jfr-revurdering"),
}
