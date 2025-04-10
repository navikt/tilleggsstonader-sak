package no.nav.tilleggsstonader.sak.behandling.historikk.domain

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandling.historikk.dto.Hendelse
import no.nav.tilleggsstonader.sak.behandling.historikk.dto.HendelseshistorikkDto
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

data class Behandlingshistorikk(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: BehandlingId,
    val steg: StegType,
    val utfall: StegUtfall? = null,
    val metadata: JsonWrapper? = null,
    val opprettetAvNavn: String = SikkerhetContext.hentSaksbehandlerNavn(),
    val opprettetAv: String = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
    val endretTid: LocalDateTime = osloNow().truncatedTo(ChronoUnit.MILLIS),
    val gitVersjon: String?,
)

fun Behandlingshistorikk.tilHendelseshistorikkDto(saksbehandling: Saksbehandling): HendelseshistorikkDto {
    val hendelse: Hendelse = mapUtfallTilHendelse() ?: mapStegTilHendelse(saksbehandling)

    val metadata =
        this.metadata.tilJson()?.toMutableMap()?.apply {
            if (saksbehandling.status.iverksetterEllerFerdigstilt() && hendelse.sattEllerTattAvVent()) {
                this.remove("kommentarSettPåVent")
                this.remove("kommentar")
            }
        }
    return HendelseshistorikkDto(
        behandlingId = this.behandlingId,
        hendelse = hendelse,
        endretAvNavn = this.opprettetAvNavn,
        endretTid = this.endretTid,
        metadata = metadata,
    )
}

private fun Behandlingshistorikk.mapUtfallTilHendelse() =
    when (this.utfall) {
        StegUtfall.SATT_PÅ_VENT -> Hendelse.SATT_PÅ_VENT
        StegUtfall.TATT_AV_VENT -> Hendelse.TATT_AV_VENT
        StegUtfall.ANGRE_SEND_TIL_BESLUTTER -> Hendelse.ANGRE_SEND_TIL_BESLUTTER
        else -> null
    }

private fun Behandlingshistorikk.mapStegTilHendelse(saksbehandling: Saksbehandling) =
    when (this.steg) {
        StegType.INNGANGSVILKÅR -> Hendelse.OPPRETTET
        StegType.SEND_TIL_BESLUTTER -> Hendelse.SENDT_TIL_BESLUTTER
        StegType.BEHANDLING_FERDIGSTILT -> mapFraFerdigstiltTilHendelse(saksbehandling.resultat)
        StegType.FERDIGSTILLE_BEHANDLING -> mapFraFerdigstilleTilHendelse(saksbehandling.resultat)
        StegType.BESLUTTE_VEDTAK -> mapFraBeslutteTilHendelse(this.utfall)
        else -> Hendelse.UKJENT
    }

fun mapFraFerdigstiltTilHendelse(resultat: BehandlingResultat): Hendelse =
    when (resultat) {
        BehandlingResultat.HENLAGT -> Hendelse.HENLAGT
        else -> Hendelse.UKJENT
    }

fun mapFraFerdigstilleTilHendelse(resultat: BehandlingResultat): Hendelse =
    when (resultat) {
        BehandlingResultat.INNVILGET, BehandlingResultat.OPPHØRT -> Hendelse.VEDTAK_IVERKSATT
        BehandlingResultat.AVSLÅTT -> Hendelse.VEDTAK_AVSLÅTT
        else -> Hendelse.UKJENT
    }

fun mapFraBeslutteTilHendelse(utfall: StegUtfall?): Hendelse =
    when (utfall) {
        StegUtfall.BESLUTTE_VEDTAK_GODKJENT -> Hendelse.VEDTAK_GODKJENT
        StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT -> Hendelse.VEDTAK_UNDERKJENT
        StegUtfall.HENLAGT -> Hendelse.HENLAGT
        else -> Hendelse.UKJENT
    }

fun JsonWrapper?.tilJson(): Map<String, Any>? = this?.json?.let { objectMapper.readValue(it) }

enum class StegUtfall {
    UTREDNING_PÅBEGYNT,
    BESLUTTE_VEDTAK_GODKJENT,
    BESLUTTE_VEDTAK_UNDERKJENT,
    HENLAGT,
    SATT_PÅ_VENT,
    TATT_AV_VENT,
    ANGRE_SEND_TIL_BESLUTTER,
}
