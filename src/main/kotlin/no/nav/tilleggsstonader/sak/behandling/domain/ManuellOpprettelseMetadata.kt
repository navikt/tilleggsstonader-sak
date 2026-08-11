package no.nav.tilleggsstonader.sak.behandling.domain

import org.springframework.data.relational.core.mapping.Column

data class ManuellOpprettelseMetadata(
    @Column("manuell_opprettelse_kilde")
    val kilde: String,
    @Column("manuell_opprettelse_beskrivelse")
    val beskrivelse: String?,
)
