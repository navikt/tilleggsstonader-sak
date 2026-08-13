package no.nav.tilleggsstonader.sak.behandling.domain

import org.springframework.data.relational.core.mapping.Column

data class ÅrsakMetadata(
    @Column("arsak_metadata_kilde")
    val kilde: ÅrsakMetadataKilde,
    @Column("arsak_metadata_beskrivelse")
    val beskrivelse: String?,
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

data class NyeOpplysningerEndringer(
    @Column("nye_opplysninger_endringer")
    val endringer: List<NyeOpplysningerEndring>,
)

enum class NyeOpplysningerEndring {
    AKTIVITET,
    MÅLGRUPPE,
    UTGIFT,
    ANNET,
}
