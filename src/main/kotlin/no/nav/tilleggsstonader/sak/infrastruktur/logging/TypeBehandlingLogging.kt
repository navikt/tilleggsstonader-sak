package no.nav.tilleggsstonader.sak.infrastruktur.logging

enum class TypeBehandlingLogging(
    val key: String,
) {
    BEHANDLING_ID("behandlingId"),
    FAGSAK_ID("fagsakId"),
    STØNADSTYPE("stonadstype"),
}
