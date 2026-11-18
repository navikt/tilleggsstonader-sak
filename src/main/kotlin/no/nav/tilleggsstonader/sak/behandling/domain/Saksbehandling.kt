package no.nav.tilleggsstonader.sak.behandling.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Aggregert behandling og fagsak
 */
data class Saksbehandling(
    override val id: BehandlingId,
    val eksternId: Long,
    override val forrigeIverksatteBehandlingId: BehandlingId? = null,
    override val type: BehandlingType,
    @Column("behandling_metode")
    override val behandlingMetode: BehandlingMetode,
    override val status: BehandlingStatus,
    override val steg: StegType,
    override val kategori: BehandlingKategori,
    @Column("arsak")
    override val årsak: BehandlingÅrsak,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
    override val årsakMetadata: ÅrsakMetadata? = null,
    override val kravMottatt: LocalDate? = null,
    override val resultat: BehandlingResultat,
    override val vedtakstidspunkt: LocalDateTime?,
    @Column("henlagt_arsak")
    override val henlagtÅrsak: HenlagtÅrsak? = null,
    override val henlagtBegrunnelse: String? = null,
    val ident: String,
    override val fagsakId: FagsakId,
    val fagsakPersonId: FagsakPersonId,
    val eksternFagsakId: Long,
    @Column("stonadstype")
    val stønadstype: Stønadstype,
    val opprettetAv: String,
    val opprettetTid: LocalDateTime,
    val endretAv: String,
    val endretTid: LocalDateTime,
) : ErBehandling {
    val skalSendeBrev: Boolean = !skalIkkeSendeBrev
    val skalIkkeSendeBrev get() = erÅrsakUtenBrev || erSatsendring
    val erÅrsakUtenBrev
        get() = årsak == BehandlingÅrsak.KORRIGERING_UTEN_BREV || årsak == BehandlingÅrsak.MANUELT_OPPRETTET_UTEN_BREV

    val harStatusOpprettet get() = status == BehandlingStatus.OPPRETTET
}
