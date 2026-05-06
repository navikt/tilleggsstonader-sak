package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

sealed interface FaktaOgVurderingReiseTilSamlingTso : FaktaOgVurdering {
    override val type: TypeFaktaOgVurderingReiseTilSamlingTso
}

sealed interface MålgruppeReiseTilSamlingTso :
    MålgruppeFaktaOgVurdering,
    FaktaOgVurderingReiseTilSamlingTso {
    override val type: MålgruppeReiseTilSamlingTsoType
}

sealed interface AktivitetReiseTilSamlingTso :
    AktivitetFaktaOgVurdering,
    FaktaOgVurderingReiseTilSamlingTso {
    override val type: AktivitetReiseTilSamlingTsoType
}

data class AAPReiseTilSamlingTso(
    override val vurderinger: VurderingAAP,
) : MålgruppeReiseTilSamlingTso {
    override val type: MålgruppeReiseTilSamlingTsoType = MålgruppeReiseTilSamlingTsoType.AAP_REISE_TIL_SAMLING_TSO
    override val fakta: IngenFakta = IngenFakta
}

data class UføretrygdReiseTilSamlingTso(
    override val vurderinger: VurderingUføretrygd,
) : MålgruppeReiseTilSamlingTso {
    override val type: MålgruppeReiseTilSamlingTsoType = MålgruppeReiseTilSamlingTsoType.UFØRETRYGD_REISE_TIL_SAMLING_TSO
    override val fakta: IngenFakta = IngenFakta
}

data class NedsattArbeidsevneReiseTilSamlingTso(
    override val vurderinger: VurderingNedsattArbeidsevne,
) : MålgruppeReiseTilSamlingTso {
    override val type: MålgruppeReiseTilSamlingTsoType = MålgruppeReiseTilSamlingTsoType.NEDSATT_ARBEIDSEVNE_REISE_TIL_SAMLING_TSO
    override val fakta: IngenFakta = IngenFakta
}

data class OmstillingsstønadReiseTilSamlingTso(
    override val vurderinger: VurderingOmstillingsstønad,
) : MålgruppeReiseTilSamlingTso {
    override val type: MålgruppeReiseTilSamlingTsoType = MålgruppeReiseTilSamlingTsoType.OMSTILLINGSSTØNAD_REISE_TIL_SAMLING_TSO
    override val fakta: IngenFakta = IngenFakta
}

data object OvergangssstønadReiseTilSamlingTso : MålgruppeReiseTilSamlingTso {
    override val type: MålgruppeReiseTilSamlingTsoType = MålgruppeReiseTilSamlingTsoType.OVERGANGSSTØNAD_REISE_TIL_SAMLING_TSO
    override val vurderinger: VurderingOvergangsstønad = VurderingOvergangsstønad
    override val fakta: IngenFakta = IngenFakta
}

data object IngenMålgruppeReiseTilSamlingTso : MålgruppeReiseTilSamlingTso {
    override val type: MålgruppeReiseTilSamlingTsoType = MålgruppeReiseTilSamlingTsoType.INGEN_MÅLGRUPPE_REISE_TIL_SAMLING_TSO
    override val vurderinger: IngenVurderinger = IngenVurderinger
    override val fakta: IngenFakta = IngenFakta
}

data class TiltakReiseTilSamlingTso(
    override val vurderinger: VurderingTiltakReiseTilSamlingTso,
) : AktivitetReiseTilSamlingTso {
    override val type: AktivitetReiseTilSamlingTsoType = AktivitetReiseTilSamlingTsoType.TILTAK_REISE_TIL_SAMLING_TSO
    override val fakta: IngenFakta = IngenFakta
}

data class UtdanningReiseTilSamlingTso(
    override val vurderinger: VurderingUtdanningReiseTilSamlingTso,
) : AktivitetReiseTilSamlingTso {
    override val type: AktivitetReiseTilSamlingTsoType = AktivitetReiseTilSamlingTsoType.UTDANNING_REISE_TIL_SAMLING_TSO
    override val fakta: IngenFakta = IngenFakta
}

data object IngenAktivitetReiseTilSamlingTso : AktivitetReiseTilSamlingTso {
    override val type: AktivitetReiseTilSamlingTsoType = AktivitetReiseTilSamlingTsoType.INGEN_AKTIVITET_REISE_TIL_SAMLING_TSO
    override val fakta: IngenFakta = IngenFakta
    override val vurderinger: Vurderinger = IngenVurderinger
}

data class VurderingTiltakReiseTilSamlingTso(
    override val lønnet: VurderingLønnet,
    override val harUtgifter: VurderingHarUtgifter,
    override val erAktivitetenObligatorisk: VurderingErAktivitetenObligatorisk,
) : HarUtgifterVurdering,
    LønnetVurdering,
    ErAktivitetenObligatoriskVurdering

data class VurderingUtdanningReiseTilSamlingTso(
    override val harUtgifter: VurderingHarUtgifter,
    override val erAktivitetenObligatorisk: VurderingErAktivitetenObligatorisk,
) : HarUtgifterVurdering,
    ErAktivitetenObligatoriskVurdering

sealed interface TypeFaktaOgVurderingReiseTilSamlingTso : TypeFaktaOgVurdering

enum class AktivitetReiseTilSamlingTsoType(
    override val vilkårperiodeType: AktivitetType,
) : TypeAktivitetOgVurdering,
    TypeFaktaOgVurderingReiseTilSamlingTso {
    UTDANNING_REISE_TIL_SAMLING_TSO(AktivitetType.UTDANNING),
    TILTAK_REISE_TIL_SAMLING_TSO(AktivitetType.TILTAK),
    INGEN_AKTIVITET_REISE_TIL_SAMLING_TSO(AktivitetType.INGEN_AKTIVITET),
}

enum class MålgruppeReiseTilSamlingTsoType(
    override val vilkårperiodeType: MålgruppeType,
) : TypeMålgruppeOgVurdering,
    TypeFaktaOgVurderingReiseTilSamlingTso {
    AAP_REISE_TIL_SAMLING_TSO(MålgruppeType.AAP),
    OMSTILLINGSSTØNAD_REISE_TIL_SAMLING_TSO(MålgruppeType.OMSTILLINGSSTØNAD),
    OVERGANGSSTØNAD_REISE_TIL_SAMLING_TSO(MålgruppeType.OVERGANGSSTØNAD),
    NEDSATT_ARBEIDSEVNE_REISE_TIL_SAMLING_TSO(MålgruppeType.NEDSATT_ARBEIDSEVNE),
    UFØRETRYGD_REISE_TIL_SAMLING_TSO(MålgruppeType.UFØRETRYGD),
    INGEN_MÅLGRUPPE_REISE_TIL_SAMLING_TSO(MålgruppeType.INGEN_MÅLGRUPPE),
}
