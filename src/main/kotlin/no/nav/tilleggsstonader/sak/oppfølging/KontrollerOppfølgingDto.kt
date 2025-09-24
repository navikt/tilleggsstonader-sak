package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
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
    val behandlingsdetaljer: OppfølgingBehandlingDetaljerDto,
)

data class OppfølgingBehandlingDetaljerDto(
    val saksnummer: Long,
    val fagsakPersonId: FagsakPersonId,
    val fagsakPersonIdent: String,
    val fagsakPersonNavn: String,
    val stønadstype: Stønadstype,
    val vedtakstidspunkt: LocalDateTime,
    val harNyereBehandling: Boolean,
)
