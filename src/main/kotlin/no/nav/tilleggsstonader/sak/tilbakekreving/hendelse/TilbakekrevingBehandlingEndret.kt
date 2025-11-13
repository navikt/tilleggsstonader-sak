package no.nav.tilleggsstonader.sak.tilbakekreving.hendelse

import java.time.LocalDateTime

data class TilbakekrevingBehandlingEndret(
    override val versjon: Int,
    val eksternFagsakId: String,
    val hendelseOpprettet: LocalDateTime,
    val eksternBehandlingId: String,
    val tilbakekreving: TilbakekrevingInfo,
) : TilbakekrevingHendelse {
    override val hendelsestype = "behandling_endret"

    fun harStatusOpprettet() = tilbakekreving.behandlingsstatus == STATUS_OPPRETTET

    fun harStatusTilBehandling() = tilbakekreving.behandlingsstatus == STATUS_TIL_BEHANDLING

    fun harStatusAvsluttet() = tilbakekreving.behandlingsstatus == STATUS_AVSLUTTET

    companion object {
        const val STATUS_OPPRETTET = "OPPRETTET"
        const val STATUS_TIL_BEHANDLING = "TIL_BEHANDLING"
        const val STATUS_AVSLUTTET = "AVSLUTTET"
    }
}

data class TilbakekrevingInfo(
    val behandlingId: String?, // Ikke påkrevd før tilbakekreving har skrudd på krav om integrasjon (fagsysteminfo_behov-hendelse)
    val sakOpprettet: LocalDateTime,
    val varselSendt: LocalDateTime?,
    val behandlingsstatus: String,
    val totaltFeilutbetaltBeløp: Int,
    val saksbehandlingURL: String,
    val fullstendigPeriode: TilbakekrevingPeriode,
)
