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

data class DagpengerFlyttingTsr(
    override val vurderinger: IngenVurderinger = IngenVurderinger,
) : MålgruppeFlyttingTsr {
    override val type: MålgruppeFlyttingTsrType = MålgruppeFlyttingTsrType.DAGPENGER_FLYTTING_TSR
    override val fakta: IngenFakta = IngenFakta
}

data class TiltakspengerFlyttingTsr(
    override val vurderinger: IngenVurderinger = IngenVurderinger,
) : MålgruppeFlyttingTsr {
    override val type: MålgruppeFlyttingTsrType = MålgruppeFlyttingTsrType.TILTAKSPENGER_FLYTTING_TSR
    override val fakta: IngenFakta = IngenFakta
}

data class UngdomsprogrammetFlyttingTsr(
    override val vurderinger: IngenVurderinger = IngenVurderinger,
) : MålgruppeFlyttingTsr {
    override val type: MålgruppeFlyttingTsrType = MålgruppeFlyttingTsrType.UNGDOMSPROGRAMMET_FLYTTING_TSR
    override val fakta: IngenFakta = IngenFakta
}

data class KvalifiseringsstønadFlyttingTsr(
    override val vurderinger: IngenVurderinger = IngenVurderinger,
) : MålgruppeFlyttingTsr {
    override val type: MålgruppeFlyttingTsrType = MålgruppeFlyttingTsrType.KVALIFISERINGSSTØNAD_FLYTTING_TSR
    override val fakta: IngenFakta = IngenFakta
}

data class InnsattIFengselFlyttingTsr(
    override val vurderinger: IngenVurderinger = IngenVurderinger,
) : MålgruppeFlyttingTsr {
    override val type: MålgruppeFlyttingTsrType = MålgruppeFlyttingTsrType.INNSATT_I_FENGSEL_FLYTTING_TSR
    override val fakta: IngenFakta = IngenFakta
}

sealed interface TypeFaktaOgVurderingFlyttingTsr : TypeFaktaOgVurdering

enum class MålgruppeFlyttingTsrType(
    override val vilkårperiodeType: MålgruppeType,
) : TypeMålgruppeOgVurdering,
    TypeFaktaOgVurderingFlyttingTsr {
    INGEN_MÅLGRUPPE_FLYTTING_TSR(MålgruppeType.INGEN_MÅLGRUPPE),
    DAGPENGER_FLYTTING_TSR(MålgruppeType.DAGPENGER),
    TILTAKSPENGER_FLYTTING_TSR(MålgruppeType.TILTAKSPENGER),
    UNGDOMSPROGRAMMET_FLYTTING_TSR(MålgruppeType.UNGDOMSPROGRAMMET),
    KVALIFISERINGSSTØNAD_FLYTTING_TSR(MålgruppeType.KVALIFISERINGSSTØNAD),
    INNSATT_I_FENGSEL_FLYTTING_TSR(MålgruppeType.INNSATT_I_FENGSEL),
}
