package no.nav.tilleggsstonader.sak.behandling.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Aggregert behandling og fagsak
 */
data class Saksbehandling(
    val id: BehandlingId,
    val eksternId: Long,
    val forrigeBehandlingId: BehandlingId? = null,
    val type: BehandlingType,
    val status: BehandlingStatus,
    val steg: StegType,
    val kategori: BehandlingKategori,
    @Column("arsak")
    val årsak: BehandlingÅrsak,
    val kravMottatt: LocalDate? = null,
    val resultat: BehandlingResultat,
    val vedtakstidspunkt: LocalDateTime?,
    @Column("henlagt_arsak")
    val henlagtÅrsak: HenlagtÅrsak? = null,
    val henlagtBegrunnelse: String? = null,
    val ident: String,
    val fagsakId: FagsakId,
    val fagsakPersonId: FagsakPersonId,
    val eksternFagsakId: Long,
    @Column("stonadstype")
    val stønadstype: Stønadstype,
    val revurderFra: LocalDate?,
    val opprettetAv: String,
    val opprettetTid: LocalDateTime,
    val endretAv: String,
    val endretTid: LocalDateTime,
) {
    val skalSendeBrev: Boolean = !skalIkkeSendeBrev
    val skalIkkeSendeBrev get() = erÅrsakUtenBrev || erSatsendring
    val erÅrsakUtenBrev
        get() = årsak == BehandlingÅrsak.KORRIGERING_UTEN_BREV || årsak == BehandlingÅrsak.MANUELT_OPPRETTET_UTEN_BREV
    val erSatsendring get() = årsak == BehandlingÅrsak.SATSENDRING

    val harStatusOpprettet get() = status == BehandlingStatus.OPPRETTET
}
