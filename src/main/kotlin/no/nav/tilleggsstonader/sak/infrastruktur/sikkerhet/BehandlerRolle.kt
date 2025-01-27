package no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet

enum class BehandlerRolle(
    val nivå: Int,
) {
    SYSTEM(4),
    BESLUTTER(3),
    SAKSBEHANDLER(2),
    VEILEDER(1),
    UKJENT(0),
}
