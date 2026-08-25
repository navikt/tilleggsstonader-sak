package no.nav.tilleggsstonader.sak.behandling.domain

import org.springframework.data.relational.core.mapping.Column

data class ÅrsakMetadata(
    @Column("arsak_metadata_kilde")
    val kilde: ÅrsakMetadataKilde,
    @Column("arsak_metadata_beskrivelse")
    val beskrivelse: String?,
    @Column("arsak_metadata_endringer")
    val endringer: List<ÅrsakMetadataEndring>,
)

enum class ÅrsakMetadataKilde {
    MODIA,
    GOSYS,
    ETTERSENDING,
    OPPFØLGINGSLISTE,
    ARENA,
    PAPIRSØKNAD,
    ANNET,
}

enum class ÅrsakMetadataEndring {
    AKTIVITET,
    MÅLGRUPPE,
    UTGIFT,
    ANNET,
}
