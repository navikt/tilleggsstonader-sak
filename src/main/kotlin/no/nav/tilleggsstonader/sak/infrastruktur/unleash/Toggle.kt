package no.nav.tilleggsstonader.sak.infrastruktur.unleash

import no.nav.tilleggsstonader.libs.unleash.ToggleId

enum class Toggle(
    override val toggleId: String,
) : ToggleId {
    KAN_OPPRETTE_BEHANDLING("sak.kan-opprette-behandling"),
    KAN_OPPRETTE_REVURDERING("sak.kan-opprette-revurdering"),

    ADMIN_KAN_OPPRETTE_BEHANDLING("sak.admin-kan-opprette-behandling"),

    HENT_BEHANDLINGER_FOR_OPPFØLGING("sak.hent-behandlinger-for-oppfoelging"),

    SKAL_VALIDERE_ÅRSAK_TIL_AVSLAG("sak.skal-validere-arsak-til-avslag"),

    SØKNAD_ROUTING_DAGLIG_REISE("sak.soknad-routing.daglig-reise"),

    OPPRETT_OPPGAVE_TILBAKEKREVING("sak.opprett-oppgave-tilbakekreving"),

    KAN_BEHANDLE_PRIVAT_BIL("sak.daglig-reise-privat-bil"),
}
