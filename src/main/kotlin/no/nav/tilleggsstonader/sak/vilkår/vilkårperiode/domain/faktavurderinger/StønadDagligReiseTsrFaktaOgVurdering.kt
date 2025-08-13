package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType

sealed interface FaktaOgVurderingDagligReiseTsr : FaktaOgVurdering {
    override val type: TypeFaktaOgVurderingDagligReiseTsr
}

sealed interface AktivitetDagligReiseTsr :
    AktivitetFaktaOgVurdering,
    FaktaOgVurderingDagligReiseTsr {
    override val type: AktivitetDagligReiseTsrType
}

data class TiltakDagligReiseTsr(
    override val vurderinger: IngenVurderinger,
) : AktivitetDagligReiseTsr {
    override val fakta: IngenFakta = IngenFakta
    override val type: AktivitetDagligReiseTsrType = AktivitetDagligReiseTsrType.TILTAK_DAGLIG_REISE_TSR
}

data object IngenAktivitetDagligReiseTsr : AktivitetDagligReiseTsr {
    override val type: AktivitetDagligReiseTsrType = AktivitetDagligReiseTsrType.INGEN_AKTIVITET_DAGLIG_REISE_TSR
    override val fakta: IngenFakta = IngenFakta
    override val vurderinger: Vurderinger = IngenVurderinger
}

sealed interface TypeFaktaOgVurderingDagligReiseTsr : TypeFaktaOgVurdering

enum class AktivitetDagligReiseTsrType(
    override val vilkårperiodeType: AktivitetType,
) : TypeAktivitetOgVurdering,
    TypeFaktaOgVurderingDagligReiseTsr {
    TILTAK_DAGLIG_REISE_TSR(AktivitetType.TILTAK),
    INGEN_AKTIVITET_DAGLIG_REISE_TSR(AktivitetType.INGEN_AKTIVITET),
}
