package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

sealed interface FaktaOgVurderingDagligReiseTsr : FaktaOgVurdering {
    override val type: TypeFaktaOgVurderingDagligReiseTsr
}

sealed interface MålgruppeDagligReiseTsr :
    MålgruppeFaktaOgVurdering,
    FaktaOgVurderingDagligReiseTsr {
    override val type: MålgruppeDagligReiseTsrType
}

sealed interface AktivitetDagligReiseTsr :
    AktivitetFaktaOgVurdering,
    FaktaOgVurderingDagligReiseTsr {
    override val type: AktivitetDagligReiseTsrType
}

data class TiltakDagligReiseTsr(
    override val vurderinger: IngenVurderinger = IngenVurderinger,
) : AktivitetDagligReiseTsr {
    override val fakta: IngenFakta = IngenFakta
    override val type: AktivitetDagligReiseTsrType = AktivitetDagligReiseTsrType.TILTAK_DAGLIG_REISE_TSR
}

data object IngenAktivitetDagligReiseTsr : AktivitetDagligReiseTsr {
    override val type: AktivitetDagligReiseTsrType = AktivitetDagligReiseTsrType.INGEN_AKTIVITET_DAGLIG_REISE_TSR
    override val fakta: IngenFakta = IngenFakta
    override val vurderinger: Vurderinger = IngenVurderinger
}

data object IngenMålgruppeDagligReiseTsr : MålgruppeDagligReiseTsr {
    override val type: MålgruppeDagligReiseTsrType = MålgruppeDagligReiseTsrType.INGEN_MÅLGRUPPE_DAGLIG_REISE_TSR
    override val vurderinger: IngenVurderinger = IngenVurderinger
    override val fakta: IngenFakta = IngenFakta
}

data class DagpengerDagligReiseTsr(
    override val vurderinger: IngenVurderinger = IngenVurderinger,
) : MålgruppeDagligReiseTsr {
    override val type: MålgruppeDagligReiseTsrType = MålgruppeDagligReiseTsrType.DAGPENGER_DAGLIG_REISE_TSR
    override val fakta: IngenFakta = IngenFakta
}

data class TiltakspengerDagligReiseTsr(
    override val vurderinger: IngenVurderinger = IngenVurderinger,
) : MålgruppeDagligReiseTsr {
    override val type: MålgruppeDagligReiseTsrType = MålgruppeDagligReiseTsrType.TILTAKSPENGER_DAGLIG_REISE_TSR
    override val fakta: IngenFakta = IngenFakta
}

data class KvalifiseringsstønadDagligReiseTsr(
    override val vurderinger: IngenVurderinger = IngenVurderinger,
) : MålgruppeDagligReiseTsr {
    override val type: MålgruppeDagligReiseTsrType = MålgruppeDagligReiseTsrType.KVALIFISERINGSSTØNAD_DAGLIG_REISE_TSR
    override val fakta: IngenFakta = IngenFakta
}

sealed interface TypeFaktaOgVurderingDagligReiseTsr : TypeFaktaOgVurdering

enum class AktivitetDagligReiseTsrType(
    override val vilkårperiodeType: AktivitetType,
) : TypeAktivitetOgVurdering,
    TypeFaktaOgVurderingDagligReiseTsr {
    TILTAK_DAGLIG_REISE_TSR(AktivitetType.TILTAK),
    INGEN_AKTIVITET_DAGLIG_REISE_TSR(AktivitetType.INGEN_AKTIVITET),
}

enum class MålgruppeDagligReiseTsrType(
    override val vilkårperiodeType: MålgruppeType,
) : TypeMålgruppeOgVurdering,
    TypeFaktaOgVurderingDagligReiseTsr {
    INGEN_MÅLGRUPPE_DAGLIG_REISE_TSR(MålgruppeType.INGEN_MÅLGRUPPE),
    DAGPENGER_DAGLIG_REISE_TSR(MålgruppeType.DAGPENGER),
    TILTAKSPENGER_DAGLIG_REISE_TSR(MålgruppeType.TILTAKSPENGER),
    KVALIFISERINGSSTØNAD_DAGLIG_REISE_TSR(MålgruppeType.KVALIFISERINGSSTØNAD),
}
