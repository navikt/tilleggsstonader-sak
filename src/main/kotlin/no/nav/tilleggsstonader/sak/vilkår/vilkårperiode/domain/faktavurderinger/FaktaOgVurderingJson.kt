package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Ønsket å legge json-spesifike saker i en egen fil for å unngå at de andre filene forsøples.
 * Prøvde å lage denne som en custom annotation men fikk det ikke til å virke [FaktaOgVurdering] extender nå [FaktaOgVurderingJson]
 */

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true,
)
@JsonSubTypes(
    JsonSubTypes.Type(UtdanningTilsynBarn::class, name = "UTDANNING_TILSYN_BARN"),
    JsonSubTypes.Type(TiltakTilsynBarn::class, name = "TILTAK_TILSYN_BARN"),
    JsonSubTypes.Type(ReellArbeidsøkerTilsynBarn::class, name = "REELL_ARBEIDSSØKER_TILSYN_BARN"),
    JsonSubTypes.Type(IngenAktivitetTilsynBarn::class, name = "INGEN_AKTIVITET_TILSYN_BARN"),

    JsonSubTypes.Type(MålgruppeTilsynBarn::class, name = "AAP_TILSYN_BARN"),
    JsonSubTypes.Type(MålgruppeTilsynBarn::class, name = "OMSTILLINGSSTØNAD_TILSYN_BARN"),
    JsonSubTypes.Type(MålgruppeTilsynBarn::class, name = "OVERGANGSSTØNAD_TILSYN_BARN"),
    JsonSubTypes.Type(MålgruppeTilsynBarn::class, name = "NEDSATT_ARBEIDSEVNE_TILSYN_BARN"),
    JsonSubTypes.Type(MålgruppeTilsynBarn::class, name = "UFØRETRYGD_TILSYN_BARN"),
    JsonSubTypes.Type(MålgruppeTilsynBarn::class, name = "SYKEPENGER_100_PROSENT_TILSYN_BARN"),
    JsonSubTypes.Type(MålgruppeTilsynBarn::class, name = "INGEN_MÅLGRUPPE_TILSYN_BARN"),
)
sealed interface FaktaOgVurderingJson
