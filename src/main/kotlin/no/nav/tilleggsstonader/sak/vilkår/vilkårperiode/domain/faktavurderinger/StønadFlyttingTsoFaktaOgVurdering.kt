package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

sealed interface FaktaOgVurderingFlyttingTso : FaktaOgVurdering {
    override val type: TypeFaktaOgVurderingFlyttingTso
}

sealed interface MålgruppeFlyttingTso :
    MålgruppeFaktaOgVurdering,
    FaktaOgVurderingFlyttingTso {
    override val type: MålgruppeFlyttingTsoType
}

data class AAPFlyttingTso(
    override val vurderinger: VurderingAAPFlyttingTso,
) : MålgruppeFlyttingTso {
    override val type: MålgruppeFlyttingTsoType = MålgruppeFlyttingTsoType.AAP_FLYTTING_TSO
    override val fakta: IngenFakta = IngenFakta
}

data class UføretrygdFlyttingTso(
    override val vurderinger: VurderingUføretrygdFlyttingTso,
) : MålgruppeFlyttingTso {
    override val type: MålgruppeFlyttingTsoType = MålgruppeFlyttingTsoType.UFØRETRYGD_FLYTTING_TSO
    override val fakta: IngenFakta = IngenFakta
}

data class NedsattArbeidsevneFlyttingTso(
    override val vurderinger: VurderingNedsattArbeidsevneFlyttingTso,
) : MålgruppeFlyttingTso {
    override val type: MålgruppeFlyttingTsoType = MålgruppeFlyttingTsoType.NEDSATT_ARBEIDSEVNE_FLYTTING_TSO
    override val fakta: IngenFakta = IngenFakta
}

data class AktivitetspengerFlyttingTso(
    override val vurderinger: VurderingAktivitetspengerFlyttingTso,
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

data object OpphørtOmstillingsstønadFlyttingTso : MålgruppeFlyttingTso {
    override val type: MålgruppeFlyttingTsoType = MålgruppeFlyttingTsoType.OPPHØRT_OMSTILLINGSSTØNAD_FLYTTING_TSO
    override val vurderinger: IngenVurderinger = IngenVurderinger
    override val fakta: IngenFakta = IngenFakta
}

data object OpphørtOvergangsstønadFlyttingTso : MålgruppeFlyttingTso {
    override val type: MålgruppeFlyttingTsoType = MålgruppeFlyttingTsoType.OPPHØRT_OVERGANGSSTØNAD_FLYTTING_TSO
    override val vurderinger: IngenVurderinger = IngenVurderinger
    override val fakta: IngenFakta = IngenFakta
}

data object IngenMålgruppeFlyttingTso : MålgruppeFlyttingTso {
    override val type: MålgruppeFlyttingTsoType = MålgruppeFlyttingTsoType.INGEN_MÅLGRUPPE_FLYTTING_TSO
    override val vurderinger: IngenVurderinger = IngenVurderinger
    override val fakta: IngenFakta = IngenFakta
}

/**
 * Aktivitet vurderes ikke for flytting TSO. Lønn i tiltaket vurderes derfor på målgruppen.
 */
data class VurderingAAPFlyttingTso(
    override val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk,
    override val aldersvilkår: VurderingAldersVilkår,
    override val lønnet: VurderingLønnet,
) : MedlemskapVurdering,
    DekketAvAnnetRegelverkVurdering,
    AldersvilkårVurdering,
    MottarSykepengerForFulltidsstillingVurdering,
    LønnetVurdering {
    override val medlemskap: VurderingMedlemskap = VurderingMedlemskap.IMPLISITT
    override val mottarSykepengerForFulltidsstilling: VurderingMottarSykepengerForFulltidsstilling =
        VurderingMottarSykepengerForFulltidsstilling.NEI_IMPLISITT
}

data class VurderingUføretrygdFlyttingTso(
    override val medlemskap: VurderingMedlemskap,
    override val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk,
    override val aldersvilkår: VurderingAldersVilkår,
    override val lønnet: VurderingLønnet,
) : MedlemskapVurdering,
    DekketAvAnnetRegelverkVurdering,
    AldersvilkårVurdering,
    MottarSykepengerForFulltidsstillingVurdering,
    LønnetVurdering {
    override val mottarSykepengerForFulltidsstilling: VurderingMottarSykepengerForFulltidsstilling =
        VurderingMottarSykepengerForFulltidsstilling.NEI_IMPLISITT
}

data class VurderingNedsattArbeidsevneFlyttingTso(
    override val medlemskap: VurderingMedlemskap,
    override val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk,
    override val aldersvilkår: VurderingAldersVilkår,
    override val mottarSykepengerForFulltidsstilling: VurderingMottarSykepengerForFulltidsstilling,
    override val lønnet: VurderingLønnet,
) : MedlemskapVurdering,
    DekketAvAnnetRegelverkVurdering,
    AldersvilkårVurdering,
    MottarSykepengerForFulltidsstillingVurdering,
    LønnetVurdering

data class VurderingAktivitetspengerFlyttingTso(
    override val lønnet: VurderingLønnet,
) : LønnetVurdering

sealed interface TypeFaktaOgVurderingFlyttingTso : TypeFaktaOgVurdering

enum class MålgruppeFlyttingTsoType(
    override val vilkårperiodeType: MålgruppeType,
) : TypeMålgruppeOgVurdering,
    TypeFaktaOgVurderingFlyttingTso {
    AAP_FLYTTING_TSO(MålgruppeType.AAP),
    OMSTILLINGSSTØNAD_FLYTTING_TSO(MålgruppeType.OMSTILLINGSSTØNAD),
    OPPHØRT_OMSTILLINGSSTØNAD_FLYTTING_TSO(MålgruppeType.OPPHØRT_OMSTILLINGSSTØNAD),
    OVERGANGSSTØNAD_FLYTTING_TSO(MålgruppeType.OVERGANGSSTØNAD),
    OPPHØRT_OVERGANGSSTØNAD_FLYTTING_TSO(MålgruppeType.OPPHØRT_OVERGANGSSTØNAD),
    NEDSATT_ARBEIDSEVNE_FLYTTING_TSO(MålgruppeType.NEDSATT_ARBEIDSEVNE),
    AKTIVITETSPENGER_FLYTTING_TSO(MålgruppeType.AKTIVITETSPENGER),
    UFØRETRYGD_FLYTTING_TSO(MålgruppeType.UFØRETRYGD),
    INGEN_MÅLGRUPPE_FLYTTING_TSO(MålgruppeType.INGEN_MÅLGRUPPE),
}
