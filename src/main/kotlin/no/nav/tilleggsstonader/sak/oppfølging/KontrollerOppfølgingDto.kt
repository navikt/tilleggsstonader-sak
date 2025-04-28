package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import java.time.LocalDateTime
import java.util.UUID

data class KontrollerOppfølgingRequest(
    val id: UUID,
    val version: Int,
    val utfall: KontrollertUtfall,
    val kommentar: String?,
)

data class KontrollerOppfølgingResponse(
    val id: UUID,
    val behandlingId: BehandlingId,
    val version: Int,
    val opprettetTidspunkt: LocalDateTime,
    val perioderTilKontroll: List<PeriodeForKontroll>,
    val kontrollert: Kontrollert?,
    val behandlingsdetaljer: Behandlingsdetaljer,
)

fun OppfølgingMedDetaljer.tilDto(): KontrollerOppfølgingResponse =
    KontrollerOppfølgingResponse(
        id = id,
        behandlingId = behandlingId,
        version = version,
        opprettetTidspunkt = opprettetTidspunkt,
        perioderTilKontroll = data.perioderTilKontroll,
        kontrollert = kontrollert,
        behandlingsdetaljer = behandlingsdetaljer,
    )
