package no.nav.tilleggsstonader.sak.statistikk.vedtak

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.AdressebeskyttelseDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.BehandlingTypeDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.BehandlingÅrsakDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.StønadstypeDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.UtbetalingerDvhV2
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.VedtakResultatDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.VedtaksperioderDvhV2
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.ÅrsakAvslagDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.ÅrsakOpphørDvh
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDateTime

/**
 * @param endretTid skal oppdateres i tilfelle man må patche data på en behandling.
 * Man skal då beholde den samme raden for å beholde opprettet_tid, men oppdatere felter og oppdatere
 */
data class VedtaksstatistikkV2(
    @Id
    val behandlingId: BehandlingId,
    val fagsakId: FagsakId,
    val eksternFagsakId: Long,
    val eksternBehandlingId: Long,
    val relatertBehandlingId: Long?, // Ekstern behandlingsid på relatert behandling
    val adressebeskyttelse: AdressebeskyttelseDvh,
    val tidspunktVedtak: LocalDateTime,
    @Column("soker_ident")
    val søkerIdent: String,
    val behandlingType: BehandlingTypeDvh,
    @Column("behandling_arsak")
    val behandlingÅrsak: BehandlingÅrsakDvh,
    val vedtakResultat: VedtakResultatDvh,
    val vedtaksperioder: VedtaksperioderDvhV2.JsonWrapper,
    val utbetalinger: UtbetalingerDvhV2.JsonWrapper,
    @Column("stonadstype")
    val stønadstype: StønadstypeDvh,
    @Column("arsaker_avslag")
    val årsakerAvslag: ÅrsakAvslagDvh.JsonWrapper?,
    @Column("arsaker_opphor")
    val årsakerOpphør: ÅrsakOpphørDvh.JsonWrapper?,
    val opprettetTid: LocalDateTime = LocalDateTime.now(),
    @LastModifiedDate
    val endretTid: LocalDateTime = opprettetTid,
)
