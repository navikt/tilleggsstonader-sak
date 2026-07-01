package no.nav.tilleggsstonader.sak.tilbakekreving.hendelse

import no.nav.tilleggsstonader.sak.tilbakekreving.domene.Tilbakekrevingsstatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

const val TILBAKEKREVING_TYPE_BEHANDLING_ENDRET = "behandling_endret"

data class TilbakekrevingBehandlingEndret(
    override val versjon: Int,
    override val eksternFagsakId: String,
    val hendelseOpprettet: OffsetDateTime,
    val eksternBehandlingId: String?,
    val tilbakekreving: TilbakekrevingInfo,
) : TilbakekrevinghendelseRecord {
    override val hendelsestype = TILBAKEKREVING_TYPE_BEHANDLING_ENDRET

    fun harStatusOpprettet() = tilbakekreving.behandlingsstatus == STATUS_OPPRETTET

    fun harStatusTilBehandling() = tilbakekreving.behandlingsstatus == STATUS_TIL_BEHANDLING

    fun harStatusAvsluttet() = tilbakekreving.behandlingsstatus == STATUS_AVSLUTTET

    fun harStatusTilForhåndsvarsel() = tilbakekreving.behandlingsstatus == STATUS_TIL_FORHÅNDSVARSEL

    fun harStatusTilGodkjenning() = tilbakekreving.behandlingsstatus == STATUS_TIL_GODKJENNING

    fun tilDomene() =
        Tilbakekrevingsstatus(
            hendelseOpprettet = hendelseOpprettet.toLocalDateTime(),
            sakOpprettet = tilbakekreving.sakOpprettet.toLocalDateTime(),
            varselSendtTidspunkt = tilbakekreving.varselSendt,
            behandlingstatus = tilbakekreving.behandlingsstatus,
            totaltFeilutbetaltBeløp = tilbakekreving.totaltFeilutbetaltBeløp,
            tilbakekrevingFom = tilbakekreving.fullstendigPeriode.fom,
            tilbakekrevingTom = tilbakekreving.fullstendigPeriode.tom,
            tilbakekrevingBehandlingId = tilbakekreving.behandlingId,
            saksbehandlingURL = tilbakekreving.saksbehandlingURL,
        )

    companion object {
        const val STATUS_OPPRETTET = "OPPRETTET"
        const val STATUS_TIL_BEHANDLING = "TIL_BEHANDLING"
        const val STATUS_TIL_FORHÅNDSVARSEL = "TIL_FORHÅNDSVARSEL"
        const val STATUS_AVSLUTTET = "AVSLUTTET"
        const val STATUS_TIL_GODKJENNING = "TIL_GODKJENNING"
    }
}

data class TilbakekrevingInfo(
    val behandlingId: String,
    val venter: TilkakrevingVenter?,
    val sakOpprettet: OffsetDateTime,
    val varselSendt: LocalDate?,
    val behandlingsstatus: String, // definert som enum hos team-tilbake. Kan type opp når vi begynner å ta i bruk
    val forrigeBehandlingsstatus: String, // definert som enum hos team-tilbake. Kan type opp når vi begynner å ta i bruk
    val totaltFeilutbetaltBeløp: BigDecimal,
    val saksbehandlingURL: String,
    val fullstendigPeriode: TilbakekrevingPeriode,
)

data class TilkakrevingVenter(
    val grunn: String, // definert som enum hos team-tilbake. Kan type opp når vi begynner å ta i bruk
    val gjenopptas: LocalDate,
)
