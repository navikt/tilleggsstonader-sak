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
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingDto(
    val id: UUID,
    val forrigeBehandlingId: UUID?,
    val fagsakId: UUID,
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
    val vedtaksdato: LocalDateTime? = null,
    val henlagtÅrsak: HenlagtÅrsak? = null,
)

fun Behandling.tilDto(stønadstype: Stønadstype): BehandlingDto =
    BehandlingDto(
        id = this.id,
        forrigeBehandlingId = this.forrigeBehandlingId,
        fagsakId = this.fagsakId,
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
    )

fun Saksbehandling.tilDto(): BehandlingDto =
    BehandlingDto(
        id = this.id,
        forrigeBehandlingId = this.forrigeBehandlingId,
        fagsakId = this.fagsakId,
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
    )
