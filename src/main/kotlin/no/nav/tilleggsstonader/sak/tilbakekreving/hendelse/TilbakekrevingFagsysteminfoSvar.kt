package no.nav.tilleggsstonader.sak.tilbakekreving.hendelse

import java.time.LocalDate
import java.time.LocalDateTime

const val TILBAKEKREVING_TYPE_FAGSYSTEMINFO_SVAR = "fagsysteminfo_svar"

data class TilbakekrevingFagsysteminfoSvar(
    override val eksternFagsakId: String,
    val hendelseOpprettet: LocalDateTime,
    val mottaker: TilbakekrevingMottaker,
    val revurdering: TilbakekrevingFagsysteminfoSvarRevurdering,
    val utvidPerioder: List<UtvidetPeriode>,
    val behandlendeEnhet: String?,
) : TilbakekrevinghendelseRecord {
    override val hendelsestype: String = TILBAKEKREVING_TYPE_FAGSYSTEMINFO_SVAR
    override val versjon: Int = 1
}

data class TilbakekrevingMottaker(
    val type: String = "PERSON",
    val ident: String,
)

data class TilbakekrevingFagsysteminfoSvarRevurdering(
    val behandlingId: String,
    val årsak: TilbakekrevingRevurderingÅrsak,
    val årsakTilFeilutbetaling: String?,
    val vedtaksdato: LocalDate,
)

data class UtvidetPeriode(
    val kravgrunnlagPeriode: TilbakekrevingPeriode,
    val vedtaksperiode: TilbakekrevingPeriode?,
)

data class TilbakekrevingPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
)

enum class TilbakekrevingRevurderingÅrsak {
    NYE_OPPLYSNINGER,
    KORRIGERING,
    KLAGE,
    UKJENT,
}
