package no.nav.tilleggsstonader.sak.statistikk.behandling.dto

enum class Hendelse {
    MOTTATT,
    PÅBEGYNT,
    VENTER,
    VEDTATT,
    BESLUTTET,
    HENLAGT,
    FERDIG,
}

enum class BehandlingMetode {
    MANUELL,
    AUTOMATISK,
    BATCH,
}
