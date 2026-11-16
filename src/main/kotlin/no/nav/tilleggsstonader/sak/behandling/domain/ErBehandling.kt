package no.nav.tilleggsstonader.sak.behandling.domain

interface ErBehandling {
    val status: BehandlingStatus
    val type: BehandlingType
    val årsak: BehandlingÅrsak

    fun erFerdigstilt(): Boolean = status == BehandlingStatus.FERDIGSTILT

    fun erKjørelisteBehandling(): Boolean = type == BehandlingType.KJØRELISTE

    val erSatsendring get() = årsak == BehandlingÅrsak.SATSENDRING
}
