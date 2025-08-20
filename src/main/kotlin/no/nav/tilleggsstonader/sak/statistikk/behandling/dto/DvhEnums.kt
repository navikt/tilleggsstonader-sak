package no.nav.tilleggsstonader.sak.statistikk.behandling.dto

enum class Hendelse {
    MOTTATT,
    PÅBEGYNT,
    VENTER,
    VEDTATT,
    ANGRET_SENDT_TIL_BESLUTTER,
    UNDERKJENT_BESLUTTER,
    BESLUTTET,
    FERDIG,
}

enum class BehandlingMetode {
    MANUELL,
    AUTOMATISK,
    BATCH,
}
