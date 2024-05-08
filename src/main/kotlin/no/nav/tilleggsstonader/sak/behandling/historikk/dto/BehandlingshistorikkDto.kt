package no.nav.tilleggsstonader.sak.behandling.historikk.dto

import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingshistorikkDto(
    val behandlingId: UUID,
    var steg: StegType,
    val endretAvNavn: String,
    val endretAv: String,
    val endretTid: LocalDateTime = osloNow(),
    val utfall: StegUtfall? = null,
    val metadata: Map<String, Any>? = null,
)
