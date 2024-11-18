package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

sealed interface FaktaOgVurderingTilsynBarn : FaktaOgVurdering {
    override val type: TypeFaktaOgVurderingTilsynBarn
}

sealed interface MålgruppeTilsynBarn : MålgruppeFaktaOgVurdering, FaktaOgVurderingTilsynBarn {
    override val type: MålgruppeTilsynBarnType
}

sealed interface AktivitetTilsynBarn : AktivitetFaktaOgVurdering, FaktaOgVurderingTilsynBarn {
    override val type: AktivitetTilsynBarnType
}

data class AAPTilsynBarn(
    override val vurderinger: VurderingAAP,
) : MålgruppeTilsynBarn {
    override val type: MålgruppeTilsynBarnType = MålgruppeTilsynBarnType.AAP_TILSYN_BARN
    override val fakta: IngenFakta = IngenFakta
}

data class UføretrygdTilsynBarn(
    override val vurderinger: VurderingUføretrygd,
) : MålgruppeTilsynBarn {
    override val type: MålgruppeTilsynBarnType = MålgruppeTilsynBarnType.UFØRETRYGD_TILSYN_BARN
    override val fakta: IngenFakta = IngenFakta
}

data class NedsattArbeidsevneTilsynBarn(
    override val vurderinger: VurderingNedsattArbeidsevne,
) : MålgruppeTilsynBarn {
    override val type: MålgruppeTilsynBarnType = MålgruppeTilsynBarnType.NEDSATT_ARBEIDSEVNE_TILSYN_BARN
    override val fakta: IngenFakta = IngenFakta
}

data class OmstillingsstønadTilsynBarn(
    override val vurderinger: VurderingOmstillingsstønad,
) : MålgruppeTilsynBarn {
    override val type: MålgruppeTilsynBarnType = MålgruppeTilsynBarnType.OMSTILLINGSSTØNAD_TILSYN_BARN
    override val fakta: IngenFakta = IngenFakta
}

data object OvergangssstønadTilsynBarn : MålgruppeTilsynBarn {
    override val type: MålgruppeTilsynBarnType = MålgruppeTilsynBarnType.OVERGANGSSTØNAD_TILSYN_BARN
    override val vurderinger: VurderingOvergangsstønad = VurderingOvergangsstønad
    override val fakta: IngenFakta = IngenFakta
}

data object IngenMålgruppeTilsynBarn : MålgruppeTilsynBarn {
    override val type: MålgruppeTilsynBarnType = MålgruppeTilsynBarnType.INGEN_MÅLGRUPPE_TILSYN_BARN
    override val vurderinger: IngenVurderinger = IngenVurderinger
    override val fakta: IngenFakta = IngenFakta
}

data object SykepengerTilsynBarn : MålgruppeTilsynBarn {
    override val type: MålgruppeTilsynBarnType = MålgruppeTilsynBarnType.SYKEPENGER_100_PROSENT_TILSYN_BARN
    override val vurderinger: IngenVurderinger = IngenVurderinger
    override val fakta: IngenFakta = IngenFakta
}

data class TiltakTilsynBarn(
    override val fakta: FaktaAktivitetTilsynBarn,
    override val vurderinger: VurderingTiltakTilsynBarn,
) : AktivitetTilsynBarn {
    override val type: AktivitetTilsynBarnType = AktivitetTilsynBarnType.TILTAK_TILSYN_BARN
}

data class UtdanningTilsynBarn(
    override val fakta: FaktaAktivitetTilsynBarn,
) : AktivitetTilsynBarn {
    override val type: AktivitetTilsynBarnType = AktivitetTilsynBarnType.UTDANNING_TILSYN_BARN
    override val vurderinger: IngenVurderinger = IngenVurderinger
}

data object IngenAktivitetTilsynBarn : AktivitetTilsynBarn {
    override val type: AktivitetTilsynBarnType = AktivitetTilsynBarnType.INGEN_AKTIVITET_TILSYN_BARN
    override val fakta: Fakta = IngenFakta
    override val vurderinger: Vurderinger = IngenVurderinger
}

data class ReellArbeidsøkerTilsynBarn(
    override val fakta: FaktaAktivitetTilsynBarn,
) : AktivitetTilsynBarn {
    override val type: AktivitetTilsynBarnType = AktivitetTilsynBarnType.REELL_ARBEIDSSØKER_TILSYN_BARN
    override val vurderinger: IngenVurderinger = IngenVurderinger
}

data class VurderingTiltakTilsynBarn(
    override val lønnet: VurderingLønnet,
) : LønnetVurdering

data class FaktaAktivitetTilsynBarn(
    override val aktivitetsdager: Int,
) : Fakta, FaktaAktivitetsdager

sealed interface TypeFaktaOgVurderingTilsynBarn : TypeFaktaOgVurdering

enum class AktivitetTilsynBarnType(
    override val vilkårperiodeType: AktivitetType,
) : TypeAktivitetOgVurdering, TypeFaktaOgVurderingTilsynBarn {

    UTDANNING_TILSYN_BARN(AktivitetType.UTDANNING),
    TILTAK_TILSYN_BARN(AktivitetType.TILTAK),
    REELL_ARBEIDSSØKER_TILSYN_BARN(AktivitetType.REELL_ARBEIDSSØKER),
    INGEN_AKTIVITET_TILSYN_BARN(AktivitetType.INGEN_AKTIVITET),
}

enum class MålgruppeTilsynBarnType(
    override val vilkårperiodeType: MålgruppeType,
) : TypeMålgruppeOgVurdering, TypeFaktaOgVurderingTilsynBarn {

    AAP_TILSYN_BARN(MålgruppeType.AAP),
    OMSTILLINGSSTØNAD_TILSYN_BARN(MålgruppeType.OMSTILLINGSSTØNAD),
    OVERGANGSSTØNAD_TILSYN_BARN(MålgruppeType.OVERGANGSSTØNAD),
    NEDSATT_ARBEIDSEVNE_TILSYN_BARN(MålgruppeType.NEDSATT_ARBEIDSEVNE),
    UFØRETRYGD_TILSYN_BARN(MålgruppeType.UFØRETRYGD),
    SYKEPENGER_100_PROSENT_TILSYN_BARN(MålgruppeType.SYKEPENGER_100_PROSENT),
    INGEN_MÅLGRUPPE_TILSYN_BARN(MålgruppeType.INGEN_MÅLGRUPPE),
}
