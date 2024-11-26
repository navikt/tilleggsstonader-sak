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

    JsonSubTypes.Type(AAPTilsynBarn::class, name = "AAP_TILSYN_BARN"),
    JsonSubTypes.Type(OmstillingsstønadTilsynBarn::class, name = "OMSTILLINGSSTØNAD_TILSYN_BARN"),
    JsonSubTypes.Type(OvergangssstønadTilsynBarn::class, name = "OVERGANGSSTØNAD_TILSYN_BARN"),
    JsonSubTypes.Type(NedsattArbeidsevneTilsynBarn::class, name = "NEDSATT_ARBEIDSEVNE_TILSYN_BARN"),
    JsonSubTypes.Type(UføretrygdTilsynBarn::class, name = "UFØRETRYGD_TILSYN_BARN"),
    JsonSubTypes.Type(SykepengerTilsynBarn::class, name = "SYKEPENGER_100_PROSENT_TILSYN_BARN"),
    JsonSubTypes.Type(IngenMålgruppeTilsynBarn::class, name = "INGEN_MÅLGRUPPE_TILSYN_BARN"),

    JsonSubTypes.Type(UtdanningLæremidler::class, name = "UTDANNING_LÆREMIDLER"),
    JsonSubTypes.Type(TiltakLæremidler::class, name = "TILTAK_LÆREMIDLER"),
    JsonSubTypes.Type(IngenAktivitetLæremidler::class, name = "INGEN_AKTIVITET_LÆREMIDLER"),

    JsonSubTypes.Type(AAPLæremidler::class, name = "AAP_LÆREMIDLER"),
    JsonSubTypes.Type(OmstillingsstønadLæremidler::class, name = "OMSTILLINGSSTØNAD_LÆREMIDLER"),
    JsonSubTypes.Type(OvergangssstønadLæremidler::class, name = "OVERGANGSSTØNAD_LÆREMIDLER"),
    JsonSubTypes.Type(NedsattArbeidsevneLæremidler::class, name = "NEDSATT_ARBEIDSEVNE_LÆREMIDLER"),
    JsonSubTypes.Type(UføretrygdLæremidler::class, name = "UFØRETRYGD_LÆREMIDLER"),
    JsonSubTypes.Type(SykepengerLæremidler::class, name = "SYKEPENGER_100_PROSENT_LÆREMIDLER"),
    JsonSubTypes.Type(IngenMålgruppeLæremidler::class, name = "INGEN_MÅLGRUPPE_LÆREMIDLER"),

    failOnRepeatedNames = true,
)
sealed interface FaktaOgVurderingJson
