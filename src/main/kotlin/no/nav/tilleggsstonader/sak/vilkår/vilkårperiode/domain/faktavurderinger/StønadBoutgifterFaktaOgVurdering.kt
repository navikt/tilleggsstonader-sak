package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType

sealed interface FaktaOgVurderingBoutgifter : FaktaOgVurdering {
    override val type: TypeFaktaOgVurderingBoutgifter
}

// TODO disse er kopiert inn fra Tilsyn Barn. Kommenter inn når implemterer målgruppe for boutgifter
// sealed interface MålgruppeBoutgifter :
//    MålgruppeFaktaOgVurdering,
//    FaktaOgVurderingBoutgifter {
//    override val type: MålgruppeBoutgifterType
// }

sealed interface AktivitetBoutgifter :
    AktivitetFaktaOgVurdering,
    FaktaOgVurderingBoutgifter {
    override val type: AktivitetBoutgifterType
}

// TODO disse er kopiert inn fra Tilsyn Barn. Kommenter inn når implemterer målgruppe for boutgifter
// data class AAPBoutgifter(
//    override val vurderinger: VurderingAAP,
// ) : MålgruppeBoutgifter {
//    override val type: MålgruppeBoutgifterType = MålgruppeBoutgifterType.AAP_TILSYN_BARN
//    override val fakta: IngenFakta = IngenFakta
// }
//
// data class UføretrygdBoutgifter(
//    override val vurderinger: VurderingUføretrygd,
// ) : MålgruppeBoutgifter {
//    override val type: MålgruppeBoutgifterType = MålgruppeBoutgifterType.UFØRETRYGD_TILSYN_BARN
//    override val fakta: IngenFakta = IngenFakta
// }
//
// data class NedsattArbeidsevneBoutgifter(
//    override val vurderinger: VurderingNedsattArbeidsevne,
// ) : MålgruppeBoutgifter {
//    override val type: MålgruppeBoutgifterType = MålgruppeBoutgifterType.NEDSATT_ARBEIDSEVNE_TILSYN_BARN
//    override val fakta: IngenFakta = IngenFakta
// }
//
// data class OmstillingsstønadBoutgifter(
//    override val vurderinger: VurderingOmstillingsstønad,
// ) : MålgruppeBoutgifter {
//    override val type: MålgruppeBoutgifterType = MålgruppeBoutgifterType.OMSTILLINGSSTØNAD_TILSYN_BARN
//    override val fakta: IngenFakta = IngenFakta
// }
//
// data object OvergangssstønadBoutgifter : MålgruppeBoutgifter {
//    override val type: MålgruppeBoutgifterType = MålgruppeBoutgifterType.OVERGANGSSTØNAD_TILSYN_BARN
//    override val vurderinger: VurderingOvergangsstønad = VurderingOvergangsstønad
//    override val fakta: IngenFakta = IngenFakta
// }
//
// data object IngenMålgruppeBoutgifter : MålgruppeBoutgifter {
//    override val type: MålgruppeBoutgifterType = MålgruppeBoutgifterType.INGEN_MÅLGRUPPE_TILSYN_BARN
//    override val vurderinger: IngenVurderinger = IngenVurderinger
//    override val fakta: IngenFakta = IngenFakta
// }
//
// data object SykepengerBoutgifter : MålgruppeBoutgifter {
//    override val type: MålgruppeBoutgifterType = MålgruppeBoutgifterType.SYKEPENGER_100_PROSENT_TILSYN_BARN
//    override val vurderinger: IngenVurderinger = IngenVurderinger
//    override val fakta: IngenFakta = IngenFakta
// }

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

// TODO disse er kopiert inn fra Tilsyn Barn. Kommenter inn når implemterer målgruppe for boutgifter
// enum class MålgruppeBoutgifterType(
//    override val vilkårperiodeType: MålgruppeType,
// ) : TypeMålgruppeOgVurdering,
//    TypeFaktaOgVurderingBoutgifter {
//    AAP_TILSYN_BARN(MålgruppeType.AAP),
//    OMSTILLINGSSTØNAD_TILSYN_BARN(MålgruppeType.OMSTILLINGSSTØNAD),
//    OVERGANGSSTØNAD_TILSYN_BARN(MålgruppeType.OVERGANGSSTØNAD),
//    NEDSATT_ARBEIDSEVNE_TILSYN_BARN(MålgruppeType.NEDSATT_ARBEIDSEVNE),
//    UFØRETRYGD_TILSYN_BARN(MålgruppeType.UFØRETRYGD),
//    SYKEPENGER_100_PROSENT_TILSYN_BARN(MålgruppeType.SYKEPENGER_100_PROSENT),
//    INGEN_MÅLGRUPPE_TILSYN_BARN(MålgruppeType.INGEN_MÅLGRUPPE),
// }
