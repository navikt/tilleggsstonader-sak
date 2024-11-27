package no.nav.tilleggsstonader.sak.fagsak.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import org.springframework.data.relational.core.mapping.Column

data class FagsakMetadata(
    val id: FagsakId,
    val eksternFagsakId: Long,
    @Column("stonadstype")
    val stønadstype: Stønadstype,
    val ident: String,
)
