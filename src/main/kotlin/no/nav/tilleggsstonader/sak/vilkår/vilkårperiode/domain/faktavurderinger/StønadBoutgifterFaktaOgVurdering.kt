package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

sealed interface FaktaOgVurderingBoutgifter : FaktaOgVurdering {
    override val type: TypeFaktaOgVurderingBoutgifter
}

sealed interface MålgruppeBoutgifter :
    MålgruppeFaktaOgVurdering,
    FaktaOgVurderingBoutgifter {
    override val type: MålgruppeBoutgifterType
}

sealed interface AktivitetBoutgifter :
    AktivitetFaktaOgVurdering,
    FaktaOgVurderingBoutgifter {
    override val type: AktivitetBoutgifterType
}

data class AAPBoutgifter(
    override val vurderinger: VurderingAAP,
) : MålgruppeBoutgifter {
    override val type: MålgruppeBoutgifterType = MålgruppeBoutgifterType.AAP_BOUTGIFTER
    override val fakta: IngenFakta = IngenFakta
}

data class UføretrygdBoutgifter(
    override val vurderinger: VurderingUføretrygd,
) : MålgruppeBoutgifter {
    override val type: MålgruppeBoutgifterType = MålgruppeBoutgifterType.UFØRETRYGD_BOUTGIFTER
    override val fakta: IngenFakta = IngenFakta
}

data class NedsattArbeidsevneBoutgifter(
    override val vurderinger: VurderingNedsattArbeidsevne,
) : MålgruppeBoutgifter {
    override val type: MålgruppeBoutgifterType = MålgruppeBoutgifterType.NEDSATT_ARBEIDSEVNE_BOUTGIFTER
    override val fakta: IngenFakta = IngenFakta
}

data class OmstillingsstønadBoutgifter(
    override val vurderinger: VurderingOmstillingsstønad,
) : MålgruppeBoutgifter {
    override val type: MålgruppeBoutgifterType = MålgruppeBoutgifterType.OMSTILLINGSSTØNAD_BOUTGIFTER
    override val fakta: IngenFakta = IngenFakta
}

data object OvergangssstønadBoutgifter : MålgruppeBoutgifter {
    override val type: MålgruppeBoutgifterType = MålgruppeBoutgifterType.OVERGANGSSTØNAD_BOUTGIFTER
    override val vurderinger: VurderingOvergangsstønad = VurderingOvergangsstønad
    override val fakta: IngenFakta = IngenFakta
}

data object IngenMålgruppeBoutgifter : MålgruppeBoutgifter {
    override val type: MålgruppeBoutgifterType = MålgruppeBoutgifterType.INGEN_MÅLGRUPPE_BOUTGIFTER
    override val vurderinger: IngenVurderinger = IngenVurderinger
    override val fakta: IngenFakta = IngenFakta
}

data class TiltakBoutgifter(
    override val vurderinger: VurderingTiltakBoutgifter,
) : AktivitetBoutgifter {
    override val fakta: IngenFakta = IngenFakta
    override val type: AktivitetBoutgifterType = AktivitetBoutgifterType.TILTAK_BOUTGIFTER
}

data object UtdanningBoutgifter : AktivitetBoutgifter {
    override val type: AktivitetBoutgifterType = AktivitetBoutgifterType.UTDANNING_BOUTGIFTER
    override val fakta: IngenFakta = IngenFakta
    override val vurderinger: IngenVurderinger = IngenVurderinger
}

data object IngenAktivitetBoutgifter : AktivitetBoutgifter {
    override val type: AktivitetBoutgifterType = AktivitetBoutgifterType.INGEN_AKTIVITET_BOUTGIFTER
    override val fakta: IngenFakta = IngenFakta
    override val vurderinger: Vurderinger = IngenVurderinger
}

data class VurderingTiltakBoutgifter(
    override val lønnet: VurderingLønnet,
) : LønnetVurdering

sealed interface TypeFaktaOgVurderingBoutgifter : TypeFaktaOgVurdering

enum class AktivitetBoutgifterType(
    override val vilkårperiodeType: AktivitetType,
) : TypeAktivitetOgVurdering,
    TypeFaktaOgVurderingBoutgifter {
    UTDANNING_BOUTGIFTER(AktivitetType.UTDANNING),
    TILTAK_BOUTGIFTER(AktivitetType.TILTAK),
    INGEN_AKTIVITET_BOUTGIFTER(AktivitetType.INGEN_AKTIVITET),
}

enum class MålgruppeBoutgifterType(
    override val vilkårperiodeType: MålgruppeType,
) : TypeMålgruppeOgVurdering,
    TypeFaktaOgVurderingBoutgifter {
    AAP_BOUTGIFTER(MålgruppeType.AAP),
    OMSTILLINGSSTØNAD_BOUTGIFTER(MålgruppeType.OMSTILLINGSSTØNAD),
    OVERGANGSSTØNAD_BOUTGIFTER(MålgruppeType.OVERGANGSSTØNAD),
    NEDSATT_ARBEIDSEVNE_BOUTGIFTER(MålgruppeType.NEDSATT_ARBEIDSEVNE),
    UFØRETRYGD_BOUTGIFTER(MålgruppeType.UFØRETRYGD),
    INGEN_MÅLGRUPPE_BOUTGIFTER(MålgruppeType.INGEN_MÅLGRUPPE),
}
