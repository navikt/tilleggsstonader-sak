package no.nav.tilleggsstonader.sak.behandling.dto

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingKategori
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.HenlagtÅrsak
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import java.time.LocalDate
import java.time.LocalDateTime

data class BehandlingsoversiktDto(
    val fagsakPersonId: FagsakPersonId,
    val tilsynBarn: FagsakMedBehandlinger?,
    val læremidler: FagsakMedBehandlinger?,
    val boUtgifter: FagsakMedBehandlinger?,
)

data class FagsakMedBehandlinger(
    val fagsakId: FagsakId,
    val eksternFagsakId: Long,
    val stønadstype: Stønadstype,
    val erLøpende: Boolean,
    val behandlinger: List<BehandlingDetaljer>,
)

data class BehandlingDetaljer(
    val id: BehandlingId,
    val forrigeIverksatteBehandlingId: BehandlingId?,
    @Deprecated("Skal bruke forrgieIverksatteBehandlingId")
    val forrigeBehandlingId: BehandlingId?,
    val fagsakId: FagsakId,
    val steg: StegType,
    val kategori: BehandlingKategori,
    val type: BehandlingType,
    val status: BehandlingStatus,
    val sistEndret: LocalDateTime,
    val resultat: BehandlingResultat,
    val opprettet: LocalDateTime,
    val opprettetAv: String,
    val behandlingsårsak: BehandlingÅrsak,
    val vedtaksdato: LocalDateTime?,
    val henlagtÅrsak: HenlagtÅrsak?,
    val henlagtBegrunnelse: String?,
    val revurderFra: LocalDate?,
    val vedtaksperiode: Vedtaksperiode?,
)

data class Vedtaksperiode(
    val fom: LocalDate?,
    val tom: LocalDate?,
)
