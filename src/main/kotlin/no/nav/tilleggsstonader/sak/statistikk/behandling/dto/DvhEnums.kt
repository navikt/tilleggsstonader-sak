package no.nav.tilleggsstonader.sak.statistikk.behandling.dto

enum class Hendelse {
    MOTTATT,
    PÅBEGYNT,
    VENTER,
    VEDTATT,
    BESLUTTET,
    FERDIG,
}

enum class BehandlingMetode {
    MANUELL,
    AUTOMATISK,
    BATCH,
}
