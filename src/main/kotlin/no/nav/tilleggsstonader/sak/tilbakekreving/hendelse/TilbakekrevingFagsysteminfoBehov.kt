package no.nav.tilleggsstonader.sak.tilbakekreving.hendelse

import java.time.LocalDateTime

const val TILBAKEKREVING_TYPE_FAGSYSTEMINFO_BEHOV = "fagsysteminfo_behov"

data class TilbakekrevingFagsysteminfoBehov(
    override val versjon: Int,
    override val eksternFagsakId: String,
    val kravgrunnlagReferanse: String?, // behandlingid
    val hendelseOpprettet: LocalDateTime,
) : TilbakekrevinghendelseRecord {
    override val hendelsestype: String = TILBAKEKREVING_TYPE_FAGSYSTEMINFO_BEHOV
}
