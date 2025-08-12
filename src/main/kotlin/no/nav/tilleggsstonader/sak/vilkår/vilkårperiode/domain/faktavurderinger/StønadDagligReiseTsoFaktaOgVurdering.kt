package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType

sealed interface FaktaOgVurderingDagligReiseTso : FaktaOgVurdering {
    override val type: TypeFaktaOgVurderingDagligReiseTso
}

sealed interface AktivitetDagligReiseTso :
    AktivitetFaktaOgVurdering,
    FaktaOgVurderingDagligReiseTso {
    override val type: AktivitetDagligReiseTsoType
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
