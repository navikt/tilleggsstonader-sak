package no.nav.tilleggsstonader.sak.infrastruktur.unleash

import no.nav.tilleggsstonader.libs.unleash.ToggleId

enum class Toggle(
    override val toggleId: String,
) : ToggleId {
    KAN_OPPRETTE_BEHANDLING("sak.kan-opprette-behandling"),
    KAN_OPPRETTE_REVURDERING("sak.kan-opprette-revurdering"),

    ADMIN_KAN_OPPRETTE_BEHANDLING("sak.admin-kan-opprette-behandling"),

    HENT_BEHANDLINGER_FOR_OPPFØLGING("sak.hent-behandlinger-for-oppfoelging"),

    SØKNAD_ROUTING_LÆREMIDLER("sak.soknad-routing.laremidler"),
    SØKNAD_ROUTING_BOUTGIFTER("sak.soknad-routing.boutgifter"),

    KAN_BRUKE_VEDTAKSPERIODER_TILSYN_BARN("sak.kan-bruke-vedtaksperioder-tilsyn-barn"),

    SKAL_HENTE_GRUNNLAG_ANNEN_FORELDER("sak.skal-hente-grunnlag-annen-forelder"),
}
