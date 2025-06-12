package no.nav.tilleggsstonader.sak.infrastruktur.unleash

import no.nav.tilleggsstonader.libs.unleash.ToggleId

enum class Toggle(
    override val toggleId: String,
) : ToggleId {
    KAN_OPPRETTE_BEHANDLING("sak.kan-opprette-behandling"),
    KAN_OPPRETTE_REVURDERING("sak.kan-opprette-revurdering"),

    ADMIN_KAN_OPPRETTE_BEHANDLING("sak.admin-kan-opprette-behandling"),

    HENT_BEHANDLINGER_FOR_OPPFØLGING("sak.hent-behandlinger-for-oppfoelging"),

    SKAL_VISE_DETALJERT_BEREGNINGSRESULTAT("sak.detaljert_beregningsresultat"),

    KAN_HA_FLERE_BEHANDLINGER_PÅ_SAMME_FAGSAK("sak.kan-ha-flere-behandlinger-på-samme-fagsak"),
}
