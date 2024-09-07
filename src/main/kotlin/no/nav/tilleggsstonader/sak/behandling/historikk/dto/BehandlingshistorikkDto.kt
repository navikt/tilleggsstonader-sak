package no.nav.tilleggsstonader.sak.behandling.historikk.dto

import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import java.time.LocalDateTime

data class BehandlingshistorikkDto(
    val behandlingId: BehandlingId,
    var steg: StegType,
    val endretAvNavn: String,
    val endretAv: String,
    val endretTid: LocalDateTime = osloNow(),
    val utfall: StegUtfall? = null,
    val metadata: Map<String, Any>? = null,
)
