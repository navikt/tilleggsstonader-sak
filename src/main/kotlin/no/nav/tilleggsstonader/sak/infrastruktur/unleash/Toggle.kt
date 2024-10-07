package no.nav.tilleggsstonader.sak.infrastruktur.unleash

import no.nav.tilleggsstonader.libs.unleash.ToggleId

enum class Toggle(override val toggleId: String) : ToggleId {
    KAN_OPPRETTE_BEHANDLING("sak.kan-opprette-behandling"),
    KAN_OPPRETTE_REVURDERING("sak.kan-opprette-revurdering"),
    REVURDERING_INNVILGE_TIDLIGERE_INNVILGET("sak.revurdering-innvilge-etter-innvilgelse"),

    AUTOMATISK_JOURNALFORING_REVURDERING("sak.automatisk-jfr-revurdering"),

    SIMULERING("sak.simulering"),

    ADMIN_KAN_OPPRETTE_BEHANDLING("sak.admin-kan-opprette-behandling"),

    HENT_BEHANDLINGER_FOR_OPPFØLGING("sak.hent-behandlinger-for-oppfoelging"),

    SØKNAD_ROUTING_LÆREMIDLER("sak.soknad-routing.laremidler"),
}
