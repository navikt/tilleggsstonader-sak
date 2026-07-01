package no.nav.tilleggsstonader.sak.tilbakekreving.dto

import no.nav.tilleggsstonader.sak.tilbakekreving.domene.Tilbakekrevingsstatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class TilbakekrevingHendelseDto(
    val hendelseOpprettet: LocalDateTime,
    val sakOpprettet: LocalDateTime,
    val varselSendtTidspunkt: LocalDate?,
    val behandlingstatus: String,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val tilbakekrevingFom: LocalDate,
    val tilbakekrevingTom: LocalDate,
    val tilbakekrevingBehandlingId: String,
    val saksbehandlingURL: String?,
)

fun Tilbakekrevingsstatus.tilDto(): TilbakekrevingHendelseDto =
    TilbakekrevingHendelseDto(
        hendelseOpprettet = this.hendelseOpprettet,
        sakOpprettet = this.sakOpprettet,
        varselSendtTidspunkt = this.varselSendtTidspunkt,
        behandlingstatus = this.behandlingstatus,
        totaltFeilutbetaltBeløp = this.totaltFeilutbetaltBeløp,
        tilbakekrevingFom = this.tilbakekrevingFom,
        tilbakekrevingTom = this.tilbakekrevingTom,
        tilbakekrevingBehandlingId = this.tilbakekrevingBehandlingId,
        saksbehandlingURL = this.saksbehandlingURL,
    )
