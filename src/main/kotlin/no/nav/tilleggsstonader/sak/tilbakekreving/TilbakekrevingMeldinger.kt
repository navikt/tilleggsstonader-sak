package no.nav.tilleggsstonader.sak.tilbakekreving

import java.time.Instant
import java.time.LocalDate

sealed interface TilbakrekrevingHendelse {
    val hendelsestype: String
    val versjon: Int
}

data class TilbakekrevingFagsysteminfoBehov(
    override val versjon: Int,
    val eksternFagsakId: String,
    val kravgrunnlagReferanse: String?, // behandlingid
    val hendelseOpprettet: Instant,
) : TilbakrekrevingHendelse {
    override val hendelsestype: String = "fagsysteminfo_behov"
}

data class TilbakekrevingFagsysteminfoSvar(
    val eksternFagsakId: String,
    val hendelseOpprettet: Instant,
    val mottaker: TilbakekrevingMottaker,
    val revurdering: TilbakekrevingFagsysteminfoSvarRevurdering,
    val utvidPerioder: List<UtvidetPeriode>,
) : TilbakrekrevingHendelse {
    override val hendelsestype: String = "fagsysteminfo_svar"
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
