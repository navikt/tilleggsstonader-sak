package no.nav.tilleggsstonader.sak.tilbakekreving.hendelse

import no.nav.tilleggsstonader.sak.tilbakekreving.domene.Tilbakekrevingsstatus
import java.time.LocalDateTime

data class TilbakekrevingBehandlingEndret(
    override val versjon: Int,
    override val eksternFagsakId: String,
    val hendelseOpprettet: LocalDateTime,
    val eksternBehandlingId: String,
    val tilbakekreving: TilbakekrevingInfo,
) : TilbakekrevinghendelseRecord {
    override val hendelsestype = "behandling_endret"

    fun harStatusOpprettet() = tilbakekreving.behandlingsstatus == STATUS_OPPRETTET

    fun harStatusTilBehandling() = tilbakekreving.behandlingsstatus == STATUS_TIL_BEHANDLING

    fun harStatusAvsluttet() = tilbakekreving.behandlingsstatus == STATUS_AVSLUTTET

    fun tilDomene() =
        Tilbakekrevingsstatus(
            hendelseOpprettet = hendelseOpprettet,
            sakOpprettet = tilbakekreving.sakOpprettet,
            varselSendtTidspunkt = tilbakekreving.varselSendt,
            behandlingstatus = tilbakekreving.behandlingsstatus,
            totaltFeilutbetaltBeløp = tilbakekreving.totaltFeilutbetaltBeløp.toLong(),
            tilbakekrevingFom = tilbakekreving.fullstendigPeriode.fom,
            tilbakekrevingTom = tilbakekreving.fullstendigPeriode.tom,
            tilbakekrevingBehandlingId = tilbakekreving.behandlingId,
        )

    companion object {
        const val STATUS_OPPRETTET = "OPPRETTET"
        const val STATUS_TIL_BEHANDLING = "TIL_BEHANDLING"
        const val STATUS_AVSLUTTET = "AVSLUTTET"
    }
}

data class TilbakekrevingInfo(
    val behandlingId: String,
    val sakOpprettet: LocalDateTime,
    val varselSendt: LocalDateTime?,
    val behandlingsstatus: String,
    val totaltFeilutbetaltBeløp: String,
    val saksbehandlingURL: String,
    val fullstendigPeriode: TilbakekrevingPeriode,
)
