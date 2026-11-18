package no.nav.tilleggsstonader.sak.behandling.domain

import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import java.time.LocalDate
import java.time.LocalDateTime

sealed interface ErBehandling {
    val id: BehandlingId
    val fagsakId: FagsakId
    val forrigeIverksatteBehandlingId: BehandlingId?
    val status: BehandlingStatus
    val type: BehandlingType
    val behandlingMetode: BehandlingMetode
    val steg: StegType
    val kategori: BehandlingKategori
    val årsak: BehandlingÅrsak
    val årsakMetadata: ÅrsakMetadata?
    val kravMottatt: LocalDate?
    val resultat: BehandlingResultat
    val henlagtÅrsak: HenlagtÅrsak?
    val henlagtBegrunnelse: String?
    val vedtakstidspunkt: LocalDateTime?

    fun kanHenlegges(): Boolean = !status.behandlingErLåstForVidereRedigering()

    fun erFerdigstilt(): Boolean = status == BehandlingStatus.FERDIGSTILT

    fun erAktiv(): Boolean = !(erFerdigstilt() || status == BehandlingStatus.SATT_PÅ_VENT)

    fun vedtakstidspunktEllerFeil(): LocalDateTime = this.vedtakstidspunkt ?: error("Mangler vedtakstidspunkt for behandling=$id")

    fun erHenlagt(): Boolean = resultat == BehandlingResultat.HENLAGT

    fun erOpphørt(): Boolean = resultat == BehandlingResultat.OPPHØRT

    fun erKjørelisteBehandling(): Boolean = type == BehandlingType.KJØRELISTE

    val erSatsendring get() = årsak == BehandlingÅrsak.SATSENDRING
}
