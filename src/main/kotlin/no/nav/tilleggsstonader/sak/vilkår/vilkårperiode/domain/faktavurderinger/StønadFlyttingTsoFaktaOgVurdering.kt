package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

sealed interface FaktaOgVurderingFlyttingTso : FaktaOgVurdering {
    override val type: TypeFaktaOgVurderingFlyttingTso
}

sealed interface MålgruppeFlyttingTso :
    MålgruppeFaktaOgVurdering,
    FaktaOgVurderingFlyttingTso {
    override val type: MålgruppeFlyttingTsoType
}

sealed interface AktivitetFlyttingTso :
    AktivitetFaktaOgVurdering,
    FaktaOgVurderingFlyttingTso {
    override val type: AktivitetFlyttingTsoType
}

data class AAPFlyttingTso(
    override val vurderinger: VurderingAAP,
) : MålgruppeFlyttingTso {
    override val type: MålgruppeFlyttingTsoType = MålgruppeFlyttingTsoType.AAP_FLYTTING_TSO
    override val fakta: IngenFakta = IngenFakta
}

data class UføretrygdFlyttingTso(
    override val vurderinger: VurderingUføretrygd,
) : MålgruppeFlyttingTso {
    override val type: MålgruppeFlyttingTsoType = MålgruppeFlyttingTsoType.UFØRETRYGD_FLYTTING_TSO
    override val fakta: IngenFakta = IngenFakta
}

data class NedsattArbeidsevneFlyttingTso(
    override val vurderinger: VurderingNedsattArbeidsevne,
) : MålgruppeFlyttingTso {
    override val type: MålgruppeFlyttingTsoType = MålgruppeFlyttingTsoType.NEDSATT_ARBEIDSEVNE_FLYTTING_TSO
    override val fakta: IngenFakta = IngenFakta
}

data class AktivitetspengerFlyttingTso(
    override val vurderinger: IngenVurderinger = IngenVurderinger,
) : MålgruppeFlyttingTso {
    override val type: MålgruppeFlyttingTsoType = MålgruppeFlyttingTsoType.AKTIVITETSPENGER_FLYTTING_TSO
    override val fakta: IngenFakta = IngenFakta
}

data class OmstillingsstønadFlyttingTso(
    override val vurderinger: VurderingOmstillingsstønad,
) : MålgruppeFlyttingTso {
    override val type: MålgruppeFlyttingTsoType = MålgruppeFlyttingTsoType.OMSTILLINGSSTØNAD_FLYTTING_TSO
    override val fakta: IngenFakta = IngenFakta
}

data object OvergangssstønadFlyttingTso : MålgruppeFlyttingTso {
    override val type: MålgruppeFlyttingTsoType = MålgruppeFlyttingTsoType.OVERGANGSSTØNAD_FLYTTING_TSO
    override val vurderinger: VurderingOvergangsstønad = VurderingOvergangsstønad
    override val fakta: IngenFakta = IngenFakta
}

data object IngenMålgruppeFlyttingTso : MålgruppeFlyttingTso {
    override val type: MålgruppeFlyttingTsoType = MålgruppeFlyttingTsoType.INGEN_MÅLGRUPPE_FLYTTING_TSO
    override val vurderinger: IngenVurderinger = IngenVurderinger
    override val fakta: IngenFakta = IngenFakta
}

data class TiltakFlyttingTso(
    override val vurderinger: VurderingTiltakFlyttingTso,
) : AktivitetFlyttingTso {
    override val type: AktivitetFlyttingTsoType = AktivitetFlyttingTsoType.TILTAK_FLYTTING_TSO
    override val fakta: IngenFakta = IngenFakta
}

data class UtdanningFlyttingTso(
    override val vurderinger: VurderingUtdanningFlyttingTso,
) : AktivitetFlyttingTso {
    override val type: AktivitetFlyttingTsoType = AktivitetFlyttingTsoType.UTDANNING_FLYTTING_TSO
    override val fakta: IngenFakta = IngenFakta
}

data object IngenAktivitetFlyttingTso : AktivitetFlyttingTso {
    override val type: AktivitetFlyttingTsoType = AktivitetFlyttingTsoType.INGEN_AKTIVITET_FLYTTING_TSO
    override val fakta: IngenFakta = IngenFakta
    override val vurderinger: Vurderinger = IngenVurderinger
}

data class VurderingTiltakFlyttingTso(
    override val lønnet: VurderingLønnet,
    override val harUtgifter: VurderingHarUtgifter,
    override val erAktivitetenObligatorisk: VurderingErAktivitetenObligatorisk,
) : HarUtgifterVurdering,
    LønnetVurdering,
    ErAktivitetenObligatoriskVurdering

data class VurderingUtdanningFlyttingTso(
    override val harUtgifter: VurderingHarUtgifter,
    override val erAktivitetenObligatorisk: VurderingErAktivitetenObligatorisk,
) : HarUtgifterVurdering,
    ErAktivitetenObligatoriskVurdering

sealed interface TypeFaktaOgVurderingFlyttingTso : TypeFaktaOgVurdering

enum class AktivitetFlyttingTsoType(
    override val vilkårperiodeType: AktivitetType,
) : TypeAktivitetOgVurdering,
    TypeFaktaOgVurderingFlyttingTso {
    UTDANNING_FLYTTING_TSO(AktivitetType.UTDANNING),
    TILTAK_FLYTTING_TSO(AktivitetType.TILTAK),
    INGEN_AKTIVITET_FLYTTING_TSO(AktivitetType.INGEN_AKTIVITET),
}

enum class MålgruppeFlyttingTsoType(
    override val vilkårperiodeType: MålgruppeType,
) : TypeMålgruppeOgVurdering,
    TypeFaktaOgVurderingFlyttingTso {
    AAP_FLYTTING_TSO(MålgruppeType.AAP),
    OMSTILLINGSSTØNAD_FLYTTING_TSO(MålgruppeType.OMSTILLINGSSTØNAD),
    OVERGANGSSTØNAD_FLYTTING_TSO(MålgruppeType.OVERGANGSSTØNAD),
    NEDSATT_ARBEIDSEVNE_FLYTTING_TSO(MålgruppeType.NEDSATT_ARBEIDSEVNE),
    AKTIVITETSPENGER_FLYTTING_TSO(MålgruppeType.AKTIVITETSPENGER),
    UFØRETRYGD_FLYTTING_TSO(MålgruppeType.UFØRETRYGD),
    INGEN_MÅLGRUPPE_FLYTTING_TSO(MålgruppeType.INGEN_MÅLGRUPPE),
}
