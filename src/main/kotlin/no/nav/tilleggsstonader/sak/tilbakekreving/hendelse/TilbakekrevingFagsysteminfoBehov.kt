package no.nav.tilleggsstonader.sak.tilbakekreving.hendelse

import java.time.LocalDateTime

data class TilbakekrevingFagsysteminfoBehov(
    override val versjon: Int,
    val eksternFagsakId: String,
    val kravgrunnlagReferanse: String?, // behandlingid
    val hendelseOpprettet: LocalDateTime,
) : TilbakekrevingHendelse {
    override val hendelsestype: String = "fagsysteminfo_behov"
}
