package no.nav.tilleggsstonader.sak.statistikk.behandling.dto

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingKategori
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import java.time.ZonedDateTime
import java.util.*

data class BehandlingsstatistikkDto(
    val behandlingId: UUID,
    val eksternBehandlingId: Long,
    val personIdent: String,
    val gjeldendeSaksbehandlerId: String,
    val beslutterId: String?,
    val eksternFagsakId: Long,
    val behandlingOpprettetTidspunkt: ZonedDateTime? = null,
    val hendelseTidspunkt: ZonedDateTime,
    val hendelse: Hendelse,
    val behandlingResultat: String? = null,
    val resultatBegrunnelse: String? = null,
    val opprettetEnhet: String,
    val ansvarligEnhet: String,
    val strengtFortroligAdresse: Boolean,
    val stønadstype: Stønadstype,
    val behandlingstype: BehandlingType,
    val behandlingÅrsak: BehandlingÅrsak,
    val henvendelseTidspunkt: ZonedDateTime? = null,
    val relatertEksternBehandlingId: Long?,
    val relatertBehandlingId: UUID?,
    val behandlingMetode: BehandlingMetode?,
    val kravMottatt: ZonedDateTime? = null,
    val årsakRevurdering: String? = null, // TODO revurdering er ikke implementert og settes til å være null
    val kategori: BehandlingKategori? = null,
)

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

enum class BehandlingKategori {
    EØS,
    NASJONAL,
}
