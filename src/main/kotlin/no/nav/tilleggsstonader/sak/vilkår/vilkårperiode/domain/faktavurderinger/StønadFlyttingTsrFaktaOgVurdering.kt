package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

sealed interface FaktaOgVurderingFlyttingTsr : FaktaOgVurdering {
    override val type: TypeFaktaOgVurderingFlyttingTsr
}

sealed interface MålgruppeFlyttingTsr :
    MålgruppeFaktaOgVurdering,
    FaktaOgVurderingFlyttingTsr {
    override val type: MålgruppeFlyttingTsrType
}

sealed interface AktivitetFlyttingTsr :
    AktivitetFaktaOgVurdering,
    FaktaOgVurderingFlyttingTsr {
    override val type: AktivitetFlyttingTsrType
}

data class TiltakFlyttingTsr(
    override val vurderinger: VurderingTiltakFlyttingTsr,
) : AktivitetFlyttingTsr {
    override val type: AktivitetFlyttingTsrType = AktivitetFlyttingTsrType.TILTAK_FLYTTING_TSR
    override val fakta: IngenFakta = IngenFakta
}

data object IngenAktivitetFlyttingTsr : AktivitetFlyttingTsr {
    override val type: AktivitetFlyttingTsrType = AktivitetFlyttingTsrType.INGEN_AKTIVITET_FLYTTING_TSR
    override val fakta: IngenFakta = IngenFakta
    override val vurderinger: Vurderinger = IngenVurderinger
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

data class VurderingTiltakFlyttingTsr(
    override val lønnet: VurderingLønnet,
    override val harUtgifter: VurderingHarUtgifter,
    override val erAktivitetenObligatorisk: VurderingErAktivitetenObligatorisk,
) : HarUtgifterVurdering,
    LønnetVurdering,
    ErAktivitetenObligatoriskVurdering

sealed interface TypeFaktaOgVurderingFlyttingTsr : TypeFaktaOgVurdering

enum class AktivitetFlyttingTsrType(
    override val vilkårperiodeType: AktivitetType,
) : TypeAktivitetOgVurdering,
    TypeFaktaOgVurderingFlyttingTsr {
    TILTAK_FLYTTING_TSR(AktivitetType.TILTAK),
    INGEN_AKTIVITET_FLYTTING_TSR(AktivitetType.INGEN_AKTIVITET),
}

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
