package no.nav.tilleggsstonader.sak.infrastruktur.unleash

import no.nav.tilleggsstonader.libs.unleash.ToggleId

enum class Toggle(
    override val toggleId: String,
) : ToggleId {
    KAN_OPPRETTE_BEHANDLING("sak.kan-opprette-behandling"),
    KAN_OPPRETTE_REVURDERING("sak.kan-opprette-revurdering"),

    ADMIN_KAN_OPPRETTE_BEHANDLING("sak.admin-kan-opprette-behandling"),

    HENT_BEHANDLINGER_FOR_OPPFØLGING("sak.hent-behandlinger-for-oppfoelging"),

    KAN_HA_FLERE_BEHANDLINGER_PÅ_SAMME_FAGSAK("sak.kan-ha-flere-behandlinger-pa-samme-fagsak"),

    TILLAT_LØPENDE_OG_MIDLERTIDIG_UTGIFT_SAMME_BEHANDLING("sak.tillat-lopende-og-midlertidig-utgift-samme-behandling"),

    SKAL_VALIDERE_ÅRSAK_TIL_AVSLAG("sak.skal-validere-arsak-til-avslag"),
    KAN_SAKSBEHANDLE_DAGLIG_REISE_TSO("sak.frontend.kan-saksbehandle.daglig-reise-tso"),
    KAN_SAKSBEHANDLE_DAGLIG_REISE_TSR("sak.frontend.kan-saksbehandle.daglig-reise-tsr"),

    SØKNAD_ROUTING_DAGLIG_REISE("sak.soknad-routing.daglig-reise"),
}
