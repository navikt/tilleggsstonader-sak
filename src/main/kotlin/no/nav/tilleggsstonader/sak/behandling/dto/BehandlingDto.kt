package no.nav.tilleggsstonader.sak.behandling.dto

import BehandlingTilJournalføringDto
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingKategori
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.HenlagtÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.NyeOpplysningerMetadata
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.opplysninger.tilordnetSaksbehandler.dto.TilordnetSaksbehandlerDto
import java.time.LocalDate
import java.time.LocalDateTime

data class BehandlingDto(
    val id: BehandlingId,
    val forrigeIverksatteBehandlingId: BehandlingId?,
    @Deprecated("Skal bruke forrgieIverksatteBehandlingId")
    val forrigeBehandlingId: BehandlingId?,
    val fagsakId: FagsakId,
    val fagsakPersonId: FagsakPersonId,
    val steg: StegType,
    val kategori: BehandlingKategori,
    val type: BehandlingType,
    val status: BehandlingStatus,
    val sistEndret: LocalDateTime,
    val resultat: BehandlingResultat,
    val opprettet: LocalDateTime,
    val opprettetAv: String,
    val behandlingsårsak: BehandlingÅrsak,
    val stønadstype: Stønadstype,
    val vedtaksdato: LocalDateTime?,
    val henlagtÅrsak: HenlagtÅrsak?,
    val henlagtBegrunnelse: String?,
    val revurderFra: LocalDate?,
    val nyeOpplysningerMetadata: NyeOpplysningerMetadata?,
    val tilordnetSaksbehandler: TilordnetSaksbehandlerDto?,
)

fun Behandling.tilDto(
    stønadstype: Stønadstype,
    fagsakPersonId: FagsakPersonId,
    tilordnetSaksbehandler: TilordnetSaksbehandlerDto?,
): BehandlingDto =
    BehandlingDto(
        id = this.id,
        forrigeIverksatteBehandlingId = this.forrigeIverksatteBehandlingId,
        forrigeBehandlingId = this.forrigeIverksatteBehandlingId,
        fagsakId = this.fagsakId,
        fagsakPersonId = fagsakPersonId,
        steg = this.steg,
        kategori = this.kategori,
        type = this.type,
        status = this.status,
        sistEndret = this.sporbar.endret.endretTid,
        resultat = this.resultat,
        opprettet = this.sporbar.opprettetTid,
        opprettetAv = this.sporbar.opprettetAv,
        behandlingsårsak = this.årsak,
        henlagtÅrsak = this.henlagtÅrsak,
        henlagtBegrunnelse = this.henlagtBegrunnelse,
        stønadstype = stønadstype,
        vedtaksdato = this.vedtakstidspunkt,
        revurderFra = this.revurderFra,
        nyeOpplysningerMetadata = this.nyeOpplysningerMetadata,
        tilordnetSaksbehandler = tilordnetSaksbehandler,
    )

fun Saksbehandling.tilDto(tilordnetSaksbehandler: TilordnetSaksbehandlerDto?): BehandlingDto =
    BehandlingDto(
        id = this.id,
        forrigeIverksatteBehandlingId = this.forrigeIverksatteBehandlingId,
        forrigeBehandlingId = this.forrigeIverksatteBehandlingId,
        fagsakId = this.fagsakId,
        fagsakPersonId = this.fagsakPersonId,
        steg = this.steg,
        kategori = this.kategori,
        type = this.type,
        status = this.status,
        sistEndret = this.endretTid,
        resultat = this.resultat,
        opprettet = this.opprettetTid,
        opprettetAv = this.opprettetAv,
        behandlingsårsak = this.årsak,
        henlagtÅrsak = this.henlagtÅrsak,
        henlagtBegrunnelse = this.henlagtBegrunnelse,
        stønadstype = stønadstype,
        vedtaksdato = this.vedtakstidspunkt,
        revurderFra = this.revurderFra,
        nyeOpplysningerMetadata = this.nyeOpplysningerMetadata,
        tilordnetSaksbehandler = tilordnetSaksbehandler,
    )

fun Behandling.tilBehandlingJournalDto(): BehandlingTilJournalføringDto =
    BehandlingTilJournalføringDto(
        id = this.id,
        type = this.type,
        status = this.status,
        resultat = this.resultat,
        behandlingsÅrsak = this.årsak,
        sistEndret = this.sporbar.endret.endretTid,
    )
