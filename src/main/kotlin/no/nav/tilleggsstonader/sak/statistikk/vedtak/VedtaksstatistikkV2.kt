package no.nav.tilleggsstonader.sak.statistikk.vedtak

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Column

/**
 * @param endretTid skal oppdateres i tilfelle man må patche data på en behandling.
 * Man skal då beholde den samme raden for å beholde opprettet_tid, men oppdatere felter og oppdatere
 */
data class VedtaksstatistikkV2(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fagsakId: FagsakId,
    val behandlingId: BehandlingId,
    val eksternFagsakId: Long,
    val eksternBehandlingId: Long,
    val relatertBehandlingId: Long?, // Ekstern behandlingsid på relatert behandling
    val adressebeskyttelse: AdressebeskyttelseDvh,
    val tidspunktVedtak: LocalDateTime,
    val person: String,
    val behandlingType: BehandlingTypeDvh,
    @Column("behandling_arsak")
    val behandlingÅrsak: BehandlingÅrsakDvh,
    val vedtakResultat: VedtakResultatDvh,
    val vedtaksperioder: VedtaksperioderDvh.JsonWrapper, //TODO oppdater
    val utbetalinger: UtbetalingerDvh.JsonWrapper, //TODO mulig oppdater
    @Column("stonadstype")
    val stønadstype: StønadstypeDvh,
    val kravMottatt: LocalDate?,
    @Column("arsaker_avslag")
    val årsakerAvslag: ÅrsakAvslagDvh.JsonWrapper?,
    @Column("arsaker_opphor")
    val årsakerOpphør: ÅrsakOpphørDvh.JsonWrapper?,
    val opprettetTid: LocalDateTime = LocalDateTime.now(),
    @LastModifiedDate
    val endretTid: LocalDateTime = opprettetTid,
    // TODO: Legg inn årsak til revurdering når revurdering kommer i løsningen
) {
    init {
        feilHvis(endretTid < opprettetTid) {
            "EndretTid=$endretTid kan ikke være før opprettetTid=$opprettetTid"
        }
    }
}
