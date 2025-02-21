package no.nav.tilleggsstonader.sak.oppfølging

import java.util.UUID

data class KontrollerOppfølgingRequest(
    val id: UUID,
    val version: Int,
    val utfall: KontrollertUtfall,
    val kommentar: String?,
)
