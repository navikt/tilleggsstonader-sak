package no.nav.tilleggsstonader.sak.behandling.dto

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingKategori
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.HenlagtÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import java.time.LocalDate
import java.time.LocalDateTime

data class BehandlingDto(
    val id: BehandlingId,
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
    val revurderFra: LocalDate?,
)

fun Behandling.tilDto(stønadstype: Stønadstype, fagsakPersonId: FagsakPersonId): BehandlingDto =
    BehandlingDto(
        id = this.id,
        forrigeBehandlingId = this.forrigeBehandlingId,
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
        stønadstype = stønadstype,
        vedtaksdato = this.vedtakstidspunkt,
        revurderFra = this.revurderFra,
    )

fun Saksbehandling.tilDto(): BehandlingDto =
    BehandlingDto(
        id = this.id,
        forrigeBehandlingId = this.forrigeBehandlingId,
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
        stønadstype = stønadstype,
        vedtaksdato = this.vedtakstidspunkt,
        revurderFra = this.revurderFra,
    )
