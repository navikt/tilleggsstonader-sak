package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

sealed interface FaktaOgVurderingDagligReiseTso : FaktaOgVurdering {
    override val type: TypeFaktaOgVurderingDagligReiseTso
}

sealed interface MålgruppeDagligReiseTso :
    MålgruppeFaktaOgVurdering,
    FaktaOgVurderingDagligReiseTso {
    override val type: MålgruppeDagligReiseTsoType
}

sealed interface AktivitetDagligReiseTso :
    AktivitetFaktaOgVurdering,
    FaktaOgVurderingDagligReiseTso {
    override val type: AktivitetDagligReiseTsoType
}

data class AAPDagligReiseTso(
    override val vurderinger: VurderingAAP,
) : MålgruppeDagligReiseTso {
    override val type: MålgruppeDagligReiseTsoType = MålgruppeDagligReiseTsoType.AAP_DAGLIG_REISE_TSO
    override val fakta: IngenFakta = IngenFakta
}

data class UføretrygdDagligReiseTso(
    override val vurderinger: VurderingUføretrygd,
) : MålgruppeDagligReiseTso {
    override val type: MålgruppeDagligReiseTsoType = MålgruppeDagligReiseTsoType.UFØRETRYGD_DAGLIG_REISE_TSO
    override val fakta: IngenFakta = IngenFakta
}

data class NedsattArbeidsevneDagligReiseTso(
    override val vurderinger: VurderingNedsattArbeidsevne,
) : MålgruppeDagligReiseTso {
    override val type: MålgruppeDagligReiseTsoType = MålgruppeDagligReiseTsoType.NEDSATT_ARBEIDSEVNE_DAGLIG_REISE_TSO
    override val fakta: IngenFakta = IngenFakta
}

data class OmstillingsstønadDagligReiseTso(
    override val vurderinger: VurderingOmstillingsstønad,
) : MålgruppeDagligReiseTso {
    override val type: MålgruppeDagligReiseTsoType = MålgruppeDagligReiseTsoType.OMSTILLINGSSTØNAD_DAGLIG_REISE_TSO
    override val fakta: IngenFakta = IngenFakta
}

data object OvergangssstønadDagligReiseTso : MålgruppeDagligReiseTso {
    override val type: MålgruppeDagligReiseTsoType = MålgruppeDagligReiseTsoType.OVERGANGSSTØNAD_DAGLIG_REISE_TSO
    override val vurderinger: VurderingOvergangsstønad = VurderingOvergangsstønad
    override val fakta: IngenFakta = IngenFakta
}

data object IngenMålgruppeDagligReiseTso : MålgruppeDagligReiseTso {
    override val type: MålgruppeDagligReiseTsoType = MålgruppeDagligReiseTsoType.INGEN_MÅLGRUPPE_DAGLIG_REISE_TSO
    override val vurderinger: IngenVurderinger = IngenVurderinger
    override val fakta: IngenFakta = IngenFakta
}

data class TiltakDagligReiseTso(
    override val vurderinger: VurderingTiltakDagligReiseTso,
) : AktivitetDagligReiseTso {
    override val fakta: IngenFakta = IngenFakta
    override val type: AktivitetDagligReiseTsoType = AktivitetDagligReiseTsoType.TILTAK_DAGLIG_REISE_TSO
}

data object UtdanningDagligReiseTso : AktivitetDagligReiseTso {
    override val type: AktivitetDagligReiseTsoType = AktivitetDagligReiseTsoType.UTDANNING_DAGLIG_REISE_TSO
    override val fakta: IngenFakta = IngenFakta
    override val vurderinger: IngenVurderinger = IngenVurderinger
}

data object IngenAktivitetDagligReiseTso : AktivitetDagligReiseTso {
    override val type: AktivitetDagligReiseTsoType = AktivitetDagligReiseTsoType.INGEN_AKTIVITET_DAGLIG_REISE_TSO
    override val fakta: IngenFakta = IngenFakta
    override val vurderinger: Vurderinger = IngenVurderinger
}

data class VurderingTiltakDagligReiseTso(
    override val lønnet: VurderingLønnet,
) : LønnetVurdering

sealed interface TypeFaktaOgVurderingDagligReiseTso : TypeFaktaOgVurdering

enum class AktivitetDagligReiseTsoType(
    override val vilkårperiodeType: AktivitetType,
) : TypeAktivitetOgVurdering,
    TypeFaktaOgVurderingDagligReiseTso {
    UTDANNING_DAGLIG_REISE_TSO(AktivitetType.UTDANNING),
    TILTAK_DAGLIG_REISE_TSO(AktivitetType.TILTAK),
    INGEN_AKTIVITET_DAGLIG_REISE_TSO(AktivitetType.INGEN_AKTIVITET),
}

enum class MålgruppeDagligReiseTsoType(
    override val vilkårperiodeType: MålgruppeType,
) : TypeMålgruppeOgVurdering,
    TypeFaktaOgVurderingDagligReiseTso {
    AAP_DAGLIG_REISE_TSO(MålgruppeType.AAP),
    OMSTILLINGSSTØNAD_DAGLIG_REISE_TSO(MålgruppeType.OMSTILLINGSSTØNAD),
    OVERGANGSSTØNAD_DAGLIG_REISE_TSO(MålgruppeType.OVERGANGSSTØNAD),
    NEDSATT_ARBEIDSEVNE_DAGLIG_REISE_TSO(MålgruppeType.NEDSATT_ARBEIDSEVNE),
    UFØRETRYGD_DAGLIG_REISE_TSO(MålgruppeType.UFØRETRYGD),
    INGEN_MÅLGRUPPE_DAGLIG_REISE_TSO(MålgruppeType.INGEN_MÅLGRUPPE),
}
