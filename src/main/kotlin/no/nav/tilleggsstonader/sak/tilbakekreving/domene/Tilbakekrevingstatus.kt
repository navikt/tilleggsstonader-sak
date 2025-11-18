package no.nav.tilleggsstonader.sak.tilbakekreving.domene

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true,
)
@JsonSubTypes(JsonSubTypes.Type(Tilbakekrevingsstatus::class, name = "status"))
sealed interface TilbakekrevingJson

data class Tilbakekrevingsstatus(
    val hendelseOpprettet: LocalDateTime,
    val sakOpprettet: LocalDateTime,
    val varselSendtTidspunkt: LocalDateTime?,
    val behandlingstatus: String,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val tilbakekrevingFom: LocalDate,
    val tilbakekrevingTom: LocalDate,
    val tilbakekrevingBehandlingId: String,
) : TilbakekrevingJson {
    val type: String = "status"
}
