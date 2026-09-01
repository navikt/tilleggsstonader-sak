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
    JsonSubTypes.Type(UtdanningPassAvBarn::class, name = "UTDANNING_TILSYN_BARN"),
    JsonSubTypes.Type(TiltakPassAvBarn::class, name = "TILTAK_TILSYN_BARN"),
    JsonSubTypes.Type(ReellArbeidsøkerPassAvBarn::class, name = "REELL_ARBEIDSSØKER_TILSYN_BARN"),
    JsonSubTypes.Type(IngenAktivitetPassAvBarn::class, name = "INGEN_AKTIVITET_TILSYN_BARN"),
    JsonSubTypes.Type(AAPPassAvBarn::class, name = "AAP_TILSYN_BARN"),
    JsonSubTypes.Type(OmstillingsstønadPassAvBarn::class, name = "OMSTILLINGSSTØNAD_TILSYN_BARN"),
    JsonSubTypes.Type(OvergangssstønadPassAvBarn::class, name = "OVERGANGSSTØNAD_TILSYN_BARN"),
    JsonSubTypes.Type(NedsattArbeidsevnePassAvBarn::class, name = "NEDSATT_ARBEIDSEVNE_TILSYN_BARN"),
    JsonSubTypes.Type(UføretrygdPassAvBarn::class, name = "UFØRETRYGD_TILSYN_BARN"),
    JsonSubTypes.Type(SykepengerPassAvBarn::class, name = "SYKEPENGER_100_PROSENT_TILSYN_BARN"),
    JsonSubTypes.Type(IngenMålgruppePassAvBarn::class, name = "INGEN_MÅLGRUPPE_TILSYN_BARN"),
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
    JsonSubTypes.Type(UtdanningBoutgifter::class, name = "UTDANNING_BOUTGIFTER"),
    JsonSubTypes.Type(TiltakBoutgifter::class, name = "TILTAK_BOUTGIFTER"),
    JsonSubTypes.Type(IngenAktivitetBoutgifter::class, name = "INGEN_AKTIVITET_BOUTGIFTER"),
    JsonSubTypes.Type(AAPBoutgifter::class, name = "AAP_BOUTGIFTER"),
    JsonSubTypes.Type(OmstillingsstønadBoutgifter::class, name = "OMSTILLINGSSTØNAD_BOUTGIFTER"),
    JsonSubTypes.Type(OvergangssstønadBoutgifter::class, name = "OVERGANGSSTØNAD_BOUTGIFTER"),
    JsonSubTypes.Type(NedsattArbeidsevneBoutgifter::class, name = "NEDSATT_ARBEIDSEVNE_BOUTGIFTER"),
    JsonSubTypes.Type(UføretrygdBoutgifter::class, name = "UFØRETRYGD_BOUTGIFTER"),
    JsonSubTypes.Type(IngenMålgruppeBoutgifter::class, name = "INGEN_MÅLGRUPPE_BOUTGIFTER"),
    JsonSubTypes.Type(TiltakDagligReiseTso::class, name = "TILTAK_DAGLIG_REISE_TSO"),
    JsonSubTypes.Type(UtdanningDagligReiseTso::class, name = "UTDANNING_DAGLIG_REISE_TSO"),
    JsonSubTypes.Type(IngenAktivitetDagligReiseTso::class, name = "INGEN_AKTIVITET_DAGLIG_REISE_TSO"),
    JsonSubTypes.Type(AAPDagligReiseTso::class, name = "AAP_DAGLIG_REISE_TSO"),
    JsonSubTypes.Type(OmstillingsstønadDagligReiseTso::class, name = "OMSTILLINGSSTØNAD_DAGLIG_REISE_TSO"),
    JsonSubTypes.Type(OvergangssstønadDagligReiseTso::class, name = "OVERGANGSSTØNAD_DAGLIG_REISE_TSO"),
    JsonSubTypes.Type(NedsattArbeidsevneDagligReiseTso::class, name = "NEDSATT_ARBEIDSEVNE_DAGLIG_REISE_TSO"),
    JsonSubTypes.Type(UføretrygdDagligReiseTso::class, name = "UFØRETRYGD_DAGLIG_REISE_TSO"),
    JsonSubTypes.Type(IngenMålgruppeDagligReiseTso::class, name = "INGEN_MÅLGRUPPE_DAGLIG_REISE_TSO"),
    JsonSubTypes.Type(TiltakDagligReiseTsr::class, name = "TILTAK_DAGLIG_REISE_TSR"),
    JsonSubTypes.Type(IngenAktivitetDagligReiseTsr::class, name = "INGEN_AKTIVITET_DAGLIG_REISE_TSR"),
    JsonSubTypes.Type(DagpengerDagligReiseTsr::class, name = "DAGPENGER_DAGLIG_REISE_TSR"),
    JsonSubTypes.Type(KvalifiseringsstønadDagligReiseTsr::class, name = "KVALIFISERINGSSTØNAD_DAGLIG_REISE_TSR"),
    JsonSubTypes.Type(TiltakspengerDagligReiseTsr::class, name = "TILTAKSPENGER_DAGLIG_REISE_TSR"),
    JsonSubTypes.Type(IngenMålgruppeDagligReiseTsr::class, name = "INGEN_MÅLGRUPPE_DAGLIG_REISE_TSR"),
    JsonSubTypes.Type(InnsattIFengselDagligReiseTsr::class, name = "INNSATT_I_FENGSEL_DAGLIG_REISE_TSR"),
    JsonSubTypes.Type(UtdanningReiseTilSamlingTso::class, name = "UTDANNING_REISE_TIL_SAMLING_TSO"),
    JsonSubTypes.Type(TiltakReiseTilSamlingTso::class, name = "TILTAK_REISE_TIL_SAMLING_TSO"),
    JsonSubTypes.Type(IngenAktivitetReiseTilSamlingTso::class, name = "INGEN_AKTIVITET_REISE_TIL_SAMLING_TSO"),
    JsonSubTypes.Type(AAPReiseTilSamlingTso::class, name = "AAP_REISE_TIL_SAMLING_TSO"),
    JsonSubTypes.Type(OmstillingsstønadReiseTilSamlingTso::class, name = "OMSTILLINGSSTØNAD_REISE_TIL_SAMLING_TSO"),
    JsonSubTypes.Type(OvergangssstønadReiseTilSamlingTso::class, name = "OVERGANGSSTØNAD_REISE_TIL_SAMLING_TSO"),
    JsonSubTypes.Type(NedsattArbeidsevneReiseTilSamlingTso::class, name = "NEDSATT_ARBEIDSEVNE_REISE_TIL_SAMLING_TSO"),
    JsonSubTypes.Type(UføretrygdReiseTilSamlingTso::class, name = "UFØRETRYGD_REISE_TIL_SAMLING_TSO"),
    JsonSubTypes.Type(IngenMålgruppeReiseTilSamlingTso::class, name = "INGEN_MÅLGRUPPE_REISE_TIL_SAMLING_TSO"),
    JsonSubTypes.Type(UtdanningReiseTilSamlingTsr::class, name = "UTDANNING_REISE_TIL_SAMLING_TSR"),
    JsonSubTypes.Type(TiltakReiseTilSamlingTsr::class, name = "TILTAK_REISE_TIL_SAMLING_TSR"),
    JsonSubTypes.Type(IngenAktivitetReiseTilSamlingTsr::class, name = "INGEN_AKTIVITET_REISE_TIL_SAMLING_TSR"),
    JsonSubTypes.Type(DagpengerReiseTilSamlingTsr::class, name = "DAGPENGER_REISE_TIL_SAMLING_TSR"),
    JsonSubTypes.Type(TiltakspengerReiseTilSamlingTsr::class, name = "TILTAKSPENGER_REISE_TIL_SAMLING_TSR"),
    JsonSubTypes.Type(KvalifiseringsstønadReiseTilSamlingTsr::class, name = "KVALIFISERINGSSTØNAD_REISE_TIL_SAMLING_TSR"),
    JsonSubTypes.Type(InnsattIFengselReiseTilSamlingTsr::class, name = "INNSATT_I_FENGSEL_REISE_TIL_SAMLING_TSR"),
    JsonSubTypes.Type(IngenMålgruppeReiseTilSamlingTsr::class, name = "INGEN_MÅLGRUPPE_REISE_TIL_SAMLING_TSR"),
    JsonSubTypes.Type(
        UtdanningReiseOppstartAvslutningHjemreiseTso::class,
        name = "UTDANNING_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO",
    ),
    JsonSubTypes.Type(
        TiltakReiseOppstartAvslutningHjemreiseTso::class,
        name = "TILTAK_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO",
    ),
    JsonSubTypes.Type(
        IngenAktivitetReiseOppstartAvslutningHjemreiseTso::class,
        name = "INGEN_AKTIVITET_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO",
    ),
    JsonSubTypes.Type(
        AAPReiseOppstartAvslutningHjemreiseTso::class,
        name = "AAP_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO",
    ),
    JsonSubTypes.Type(
        OmstillingsstønadReiseOppstartAvslutningHjemreiseTso::class,
        name = "OMSTILLINGSSTØNAD_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO",
    ),
    JsonSubTypes.Type(
        OvergangssstønadReiseOppstartAvslutningHjemreiseTso::class,
        name = "OVERGANGSSTØNAD_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO",
    ),
    JsonSubTypes.Type(
        NedsattArbeidsevneReiseOppstartAvslutningHjemreiseTso::class,
        name = "NEDSATT_ARBEIDSEVNE_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO",
    ),
    JsonSubTypes.Type(
        UføretrygdReiseOppstartAvslutningHjemreiseTso::class,
        name = "UFØRETRYGD_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO",
    ),
    JsonSubTypes.Type(
        IngenMålgruppeReiseOppstartAvslutningHjemreiseTso::class,
        name = "INGEN_MÅLGRUPPE_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO",
    ),
    JsonSubTypes.Type(
        TiltakReiseOppstartAvslutningHjemreiseTsr::class,
        name = "TILTAK_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR",
    ),
    JsonSubTypes.Type(
        IngenAktivitetReiseOppstartAvslutningHjemreiseTsr::class,
        name = "INGEN_AKTIVITET_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR",
    ),
    JsonSubTypes.Type(
        DagpengerReiseOppstartAvslutningHjemreiseTsr::class,
        name = "DAGPENGER_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR",
    ),
    JsonSubTypes.Type(
        TiltakspengerReiseOppstartAvslutningHjemreiseTsr::class,
        name = "TILTAKSPENGER_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR",
    ),
    JsonSubTypes.Type(
        KvalifiseringsstønadReiseOppstartAvslutningHjemreiseTsr::class,
        name = "KVALIFISERINGSSTØNAD_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR",
    ),
    JsonSubTypes.Type(
        InnsattIFengselReiseOppstartAvslutningHjemreiseTsr::class,
        name = "INNSATT_I_FENGSEL_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR",
    ),
    JsonSubTypes.Type(
        IngenMålgruppeReiseOppstartAvslutningHjemreiseTsr::class,
        name = "INGEN_MÅLGRUPPE_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR",
    ),
    failOnRepeatedNames = true,
)
sealed interface FaktaOgVurderingJson
