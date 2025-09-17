package no.nav.tilleggsstonader.sak.tilbakekreving

import java.time.Instant

data class TilbakekrevingKravgrunnlagOppslagRecord(
    val eksternFagsakId: String,
    val kravgrunnlagReferanse: String?, // behandlingid
    val hendelseOpprettet: Instant,
    val hendelsestype: String,
    val versjon: Int,
)
