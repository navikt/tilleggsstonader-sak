package no.nav.tilleggsstonader.sak.infrastruktur.unleash

import no.nav.tilleggsstonader.libs.unleash.ToggleId

enum class Toggle(override val toggleId: String) : ToggleId {
    KAN_OPPRETTE_BEHANDLING("sak.kan-opprette-behandling"),
    KAN_OPPRETTE_BEHANDLING_FRA_JOURNALPOST("sak.kan-opprette-behandling-fra-journalpost"),
    KAN_OPPRETTE_REVURDERING("sak.kan-opprette-revurdering"),
    REVURDERING_INNVILGE_TIDLIGERE_INNVILGET("sak.revurdering-innvilge-etter-innvilgelse"),

    AUTOMATISK_JOURNALFORING_REVURDERING("sak.automatisk-jfr-revurdering"),
    MANUELL_JOURNALFØRING_TIDLIGERE_INNVILGET("sak.manuell-jfr-tidligere-innvilget-behandling"),

    SIMULERING("sak.simulering"),

    ARENA_STATUS_FAGSAK_PERSON("sak.arena-status-fagsak-person"),
}
