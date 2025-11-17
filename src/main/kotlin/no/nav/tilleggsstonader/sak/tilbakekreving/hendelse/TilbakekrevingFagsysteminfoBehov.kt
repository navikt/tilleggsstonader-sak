package no.nav.tilleggsstonader.sak.tilbakekreving.hendelse

import java.time.LocalDateTime

data class TilbakekrevingFagsysteminfoBehov(
    override val versjon: Int,
    override val eksternFagsakId: String,
    val kravgrunnlagReferanse: String?, // behandlingid
    val hendelseOpprettet: LocalDateTime,
) : TilbakekrevinghendelseRecord {
    override val hendelsestype: String = "fagsysteminfo_behov"
}
