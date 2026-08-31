package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

sealed interface FaktaOgVurderingReiseOppstartAvslutningHjemreiseTso : FaktaOgVurdering {
    override val type: TypeFaktaOgVurderingReiseOppstartAvslutningHjemreiseTso
}

sealed interface MålgruppeReiseOppstartAvslutningHjemreiseTso :
    MålgruppeFaktaOgVurdering,
    FaktaOgVurderingReiseOppstartAvslutningHjemreiseTso {
    override val type: MålgruppeReiseOppstartAvslutningHjemreiseTsoType
}

sealed interface AktivitetReiseOppstartAvslutningHjemreiseTso :
    AktivitetFaktaOgVurdering,
    FaktaOgVurderingReiseOppstartAvslutningHjemreiseTso {
    override val type: AktivitetReiseOppstartAvslutningHjemreiseTsoType
}

data class AAPReiseOppstartAvslutningHjemreiseTso(
    override val vurderinger: VurderingAAP,
) : MålgruppeReiseOppstartAvslutningHjemreiseTso {
    override val type: MålgruppeReiseOppstartAvslutningHjemreiseTsoType =
        MålgruppeReiseOppstartAvslutningHjemreiseTsoType.AAP_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO
    override val fakta: IngenFakta = IngenFakta
}

data class UføretrygdReiseOppstartAvslutningHjemreiseTso(
    override val vurderinger: VurderingUføretrygd,
) : MålgruppeReiseOppstartAvslutningHjemreiseTso {
    override val type: MålgruppeReiseOppstartAvslutningHjemreiseTsoType =
        MålgruppeReiseOppstartAvslutningHjemreiseTsoType.UFØRETRYGD_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO
    override val fakta: IngenFakta = IngenFakta
}

data class NedsattArbeidsevneReiseOppstartAvslutningHjemreiseTso(
    override val vurderinger: VurderingNedsattArbeidsevne,
) : MålgruppeReiseOppstartAvslutningHjemreiseTso {
    override val type: MålgruppeReiseOppstartAvslutningHjemreiseTsoType =
        MålgruppeReiseOppstartAvslutningHjemreiseTsoType.NEDSATT_ARBEIDSEVNE_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO
    override val fakta: IngenFakta = IngenFakta
}

data class OmstillingsstønadReiseOppstartAvslutningHjemreiseTso(
    override val vurderinger: VurderingOmstillingsstønad,
) : MålgruppeReiseOppstartAvslutningHjemreiseTso {
    override val type: MålgruppeReiseOppstartAvslutningHjemreiseTsoType =
        MålgruppeReiseOppstartAvslutningHjemreiseTsoType.OMSTILLINGSSTØNAD_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO
    override val fakta: IngenFakta = IngenFakta
}

data object OvergangssstønadReiseOppstartAvslutningHjemreiseTso : MålgruppeReiseOppstartAvslutningHjemreiseTso {
    override val type: MålgruppeReiseOppstartAvslutningHjemreiseTsoType =
        MålgruppeReiseOppstartAvslutningHjemreiseTsoType.OVERGANGSSTØNAD_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO
    override val vurderinger: VurderingOvergangsstønad = VurderingOvergangsstønad
    override val fakta: IngenFakta = IngenFakta
}

data object IngenMålgruppeReiseOppstartAvslutningHjemreiseTso : MålgruppeReiseOppstartAvslutningHjemreiseTso {
    override val type: MålgruppeReiseOppstartAvslutningHjemreiseTsoType =
        MålgruppeReiseOppstartAvslutningHjemreiseTsoType.INGEN_MÅLGRUPPE_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO
    override val vurderinger: IngenVurderinger = IngenVurderinger
    override val fakta: IngenFakta = IngenFakta
}

data class TiltakReiseOppstartAvslutningHjemreiseTso(
    override val vurderinger: VurderingTiltakReiseOppstartAvslutningHjemreiseTso,
) : AktivitetReiseOppstartAvslutningHjemreiseTso {
    override val type: AktivitetReiseOppstartAvslutningHjemreiseTsoType =
        AktivitetReiseOppstartAvslutningHjemreiseTsoType.TILTAK_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO
    override val fakta: IngenFakta = IngenFakta
}

data class UtdanningReiseOppstartAvslutningHjemreiseTso(
    override val vurderinger: VurderingUtdanningReiseOppstartAvslutningHjemreiseTso,
) : AktivitetReiseOppstartAvslutningHjemreiseTso {
    override val type: AktivitetReiseOppstartAvslutningHjemreiseTsoType =
        AktivitetReiseOppstartAvslutningHjemreiseTsoType.UTDANNING_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO
    override val fakta: IngenFakta = IngenFakta
}

data object IngenAktivitetReiseOppstartAvslutningHjemreiseTso : AktivitetReiseOppstartAvslutningHjemreiseTso {
    override val type: AktivitetReiseOppstartAvslutningHjemreiseTsoType =
        AktivitetReiseOppstartAvslutningHjemreiseTsoType.INGEN_AKTIVITET_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO
    override val fakta: IngenFakta = IngenFakta
    override val vurderinger: Vurderinger = IngenVurderinger
}

data class VurderingTiltakReiseOppstartAvslutningHjemreiseTso(
    override val lønnet: VurderingLønnet,
    override val harUtgifter: VurderingHarUtgifter,
    override val erAktivitetenObligatorisk: VurderingErAktivitetenObligatorisk,
) : HarUtgifterVurdering,
    LønnetVurdering,
    ErAktivitetenObligatoriskVurdering

data class VurderingUtdanningReiseOppstartAvslutningHjemreiseTso(
    override val harUtgifter: VurderingHarUtgifter,
    override val erAktivitetenObligatorisk: VurderingErAktivitetenObligatorisk,
) : HarUtgifterVurdering,
    ErAktivitetenObligatoriskVurdering

sealed interface TypeFaktaOgVurderingReiseOppstartAvslutningHjemreiseTso : TypeFaktaOgVurdering

enum class AktivitetReiseOppstartAvslutningHjemreiseTsoType(
    override val vilkårperiodeType: AktivitetType,
) : TypeAktivitetOgVurdering,
    TypeFaktaOgVurderingReiseOppstartAvslutningHjemreiseTso {
    UTDANNING_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO(AktivitetType.UTDANNING),
    TILTAK_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO(AktivitetType.TILTAK),
    INGEN_AKTIVITET_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO(AktivitetType.INGEN_AKTIVITET),
}

enum class MålgruppeReiseOppstartAvslutningHjemreiseTsoType(
    override val vilkårperiodeType: MålgruppeType,
) : TypeMålgruppeOgVurdering,
    TypeFaktaOgVurderingReiseOppstartAvslutningHjemreiseTso {
    AAP_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO(MålgruppeType.AAP),
    OMSTILLINGSSTØNAD_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO(MålgruppeType.OMSTILLINGSSTØNAD),
    OVERGANGSSTØNAD_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO(MålgruppeType.OVERGANGSSTØNAD),
    NEDSATT_ARBEIDSEVNE_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO(MålgruppeType.NEDSATT_ARBEIDSEVNE),
    UFØRETRYGD_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO(MålgruppeType.UFØRETRYGD),
    INGEN_MÅLGRUPPE_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO(MålgruppeType.INGEN_MÅLGRUPPE),
}
