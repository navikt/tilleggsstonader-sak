package no.nav.tilleggsstonader.sak.behandling.domain

import org.springframework.data.relational.core.mapping.Column

data class NyeOpplysningerMetadata(
    @Column("nye_opplysninger_kilde")
    val kilde: NyeOpplysningerKilde,
    @Column("nye_opplysninger_endringer")
    val endringer: List<NyeOpplysningerEndring>,
    @Column("nye_opplysninger_beskrivelse")
    val beskrivelse: String?,
)

enum class NyeOpplysningerKilde {
    MODIA,
    GOSYS,
    ETTERSENDING,
    OPPFØLGINGSLISTE,
    ANNET,
}

enum class NyeOpplysningerEndring {
    AKTIVITET,
    MÅLGRUPPE,
    UTGIFT,
    ANNET,
}
