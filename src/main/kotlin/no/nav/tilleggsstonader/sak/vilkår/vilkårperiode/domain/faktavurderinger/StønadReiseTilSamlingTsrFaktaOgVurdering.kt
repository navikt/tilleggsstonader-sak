package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

sealed interface FaktaOgVurderingReiseTilSamlingTsr : FaktaOgVurdering {
    override val type: TypeFaktaOgVurderingReiseTilSamlingTsr
}

sealed interface MålgruppeReiseTilSamlingTsr :
    MålgruppeFaktaOgVurdering,
    FaktaOgVurderingReiseTilSamlingTsr {
    override val type: MålgruppeReiseTilSamlingTsrType
}

sealed interface AktivitetReiseTilSamlingTsr :
    AktivitetFaktaOgVurdering,
    FaktaOgVurderingReiseTilSamlingTsr {
    override val type: AktivitetReiseTilSamlingTsrType
}

data object TiltakReiseTilSamlingTsr : AktivitetReiseTilSamlingTsr {
    override val type: AktivitetReiseTilSamlingTsrType =
        AktivitetReiseTilSamlingTsrType.TILTAK_REISE_TIL_SAMLING_TSR
    override val fakta: IngenFakta = IngenFakta
    override val vurderinger: Vurderinger = IngenVurderinger
}

data object UtdanningReiseTilSamlingTsr : AktivitetReiseTilSamlingTsr {
    override val type: AktivitetReiseTilSamlingTsrType =
        AktivitetReiseTilSamlingTsrType.UTDANNING_REISE_TIL_SAMLING_TSR
    override val fakta: IngenFakta = IngenFakta
    override val vurderinger: Vurderinger = IngenVurderinger
}

data object IngenAktivitetReiseTilSamlingTsr : AktivitetReiseTilSamlingTsr {
    override val type: AktivitetReiseTilSamlingTsrType =
        AktivitetReiseTilSamlingTsrType.INGEN_AKTIVITET_REISE_TIL_SAMLING_TSR
    override val fakta: IngenFakta = IngenFakta
    override val vurderinger: Vurderinger = IngenVurderinger
}

data object IngenMålgruppeReiseTilSamlingTsr : MålgruppeReiseTilSamlingTsr {
    override val type: MålgruppeReiseTilSamlingTsrType =
        MålgruppeReiseTilSamlingTsrType.INGEN_MÅLGRUPPE_REISE_TIL_SAMLING_TSR
    override val vurderinger: IngenVurderinger = IngenVurderinger
    override val fakta: IngenFakta = IngenFakta
}

data class DagpengerReiseTilSamlingTsr(
    override val vurderinger: IngenVurderinger = IngenVurderinger,
) : MålgruppeReiseTilSamlingTsr {
    override val type: MålgruppeReiseTilSamlingTsrType =
        MålgruppeReiseTilSamlingTsrType.DAGPENGER_REISE_TIL_SAMLING_TSR
    override val fakta: IngenFakta = IngenFakta
}

data class TiltakspengerReiseTilSamlingTsr(
    override val vurderinger: IngenVurderinger = IngenVurderinger,
) : MålgruppeReiseTilSamlingTsr {
    override val type: MålgruppeReiseTilSamlingTsrType =
        MålgruppeReiseTilSamlingTsrType.TILTAKSPENGER_REISE_TIL_SAMLING_TSR
    override val fakta: IngenFakta = IngenFakta
}

data class KvalifiseringsstønadReiseTilSamlingTsr(
    override val vurderinger: IngenVurderinger = IngenVurderinger,
) : MålgruppeReiseTilSamlingTsr {
    override val type: MålgruppeReiseTilSamlingTsrType =
        MålgruppeReiseTilSamlingTsrType.KVALIFISERINGSSTØNAD_REISE_TIL_SAMLING_TSR
    override val fakta: IngenFakta = IngenFakta
}

data class InnsattIFengselReiseTilSamlingTsr(
    override val vurderinger: IngenVurderinger = IngenVurderinger,
) : MålgruppeReiseTilSamlingTsr {
    override val type: MålgruppeReiseTilSamlingTsrType =
        MålgruppeReiseTilSamlingTsrType.INNSATT_I_FENGSEL_REISE_TIL_SAMLING_TSR
    override val fakta: IngenFakta = IngenFakta
}

sealed interface TypeFaktaOgVurderingReiseTilSamlingTsr : TypeFaktaOgVurdering

enum class AktivitetReiseTilSamlingTsrType(
    override val vilkårperiodeType: AktivitetType,
) : TypeAktivitetOgVurdering,
    TypeFaktaOgVurderingReiseTilSamlingTsr {
    UTDANNING_REISE_TIL_SAMLING_TSR(AktivitetType.UTDANNING),
    TILTAK_REISE_TIL_SAMLING_TSR(AktivitetType.TILTAK),
    INGEN_AKTIVITET_REISE_TIL_SAMLING_TSR(AktivitetType.INGEN_AKTIVITET),
}

enum class MålgruppeReiseTilSamlingTsrType(
    override val vilkårperiodeType: MålgruppeType,
) : TypeMålgruppeOgVurdering,
    TypeFaktaOgVurderingReiseTilSamlingTsr {
    INGEN_MÅLGRUPPE_REISE_TIL_SAMLING_TSR(MålgruppeType.INGEN_MÅLGRUPPE),
    DAGPENGER_REISE_TIL_SAMLING_TSR(MålgruppeType.DAGPENGER),
    TILTAKSPENGER_REISE_TIL_SAMLING_TSR(MålgruppeType.TILTAKSPENGER),
    KVALIFISERINGSSTØNAD_REISE_TIL_SAMLING_TSR(MålgruppeType.KVALIFISERINGSSTØNAD),
    INNSATT_I_FENGSEL_REISE_TIL_SAMLING_TSR(MålgruppeType.INNSATT_I_FENGSEL),
}
