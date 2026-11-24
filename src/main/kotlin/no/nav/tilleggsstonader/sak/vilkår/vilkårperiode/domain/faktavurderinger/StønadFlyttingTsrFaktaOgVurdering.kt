package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

sealed interface FaktaOgVurderingFlyttingTsr : FaktaOgVurdering {
    override val type: TypeFaktaOgVurderingFlyttingTsr
}

sealed interface MålgruppeFlyttingTsr :
    MålgruppeFaktaOgVurdering,
    FaktaOgVurderingFlyttingTsr {
    override val type: MålgruppeFlyttingTsrType
}

data object IngenMålgruppeFlyttingTsr : MålgruppeFlyttingTsr {
    override val type: MålgruppeFlyttingTsrType = MålgruppeFlyttingTsrType.INGEN_MÅLGRUPPE_FLYTTING_TSR
    override val vurderinger: IngenVurderinger = IngenVurderinger
    override val fakta: IngenFakta = IngenFakta
}

data class ArbeidssøkerFlyttingTsr(
    override val vurderinger: IngenVurderinger = IngenVurderinger,
) : MålgruppeFlyttingTsr {
    override val type: MålgruppeFlyttingTsrType = MålgruppeFlyttingTsrType.ARBEIDSSØKER_FLYTTING_TSR
    override val fakta: IngenFakta = IngenFakta
}

sealed interface TypeFaktaOgVurderingFlyttingTsr : TypeFaktaOgVurdering

enum class MålgruppeFlyttingTsrType(
    override val vilkårperiodeType: MålgruppeType,
) : TypeMålgruppeOgVurdering,
    TypeFaktaOgVurderingFlyttingTsr {
    INGEN_MÅLGRUPPE_FLYTTING_TSR(MålgruppeType.INGEN_MÅLGRUPPE),
    ARBEIDSSØKER_FLYTTING_TSR(MålgruppeType.ARBEIDSSØKER),
}
