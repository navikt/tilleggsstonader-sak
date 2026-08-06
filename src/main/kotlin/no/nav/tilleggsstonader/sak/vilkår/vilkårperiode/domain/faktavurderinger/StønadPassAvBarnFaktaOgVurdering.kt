package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

sealed interface FaktaOgVurderingPassAvBarn : FaktaOgVurdering {
    override val type: TypeFaktaOgVurderingPassAvBarn
}

sealed interface MålgruppePassAvBarn :
    MålgruppeFaktaOgVurdering,
    FaktaOgVurderingPassAvBarn {
    override val type: MålgruppePassAvBarnType
}

sealed interface AktivitetPassAvBarn :
    AktivitetFaktaOgVurdering,
    FaktaOgVurderingPassAvBarn {
    override val type: AktivitetPassAvBarnType
}

data class AAPPassAvBarn(
    override val vurderinger: VurderingAAP,
) : MålgruppePassAvBarn {
    override val type: MålgruppePassAvBarnType = MålgruppePassAvBarnType.AAP_TILSYN_BARN
    override val fakta: IngenFakta = IngenFakta
}

data class UføretrygdPassAvBarn(
    override val vurderinger: VurderingUføretrygd,
) : MålgruppePassAvBarn {
    override val type: MålgruppePassAvBarnType = MålgruppePassAvBarnType.UFØRETRYGD_TILSYN_BARN
    override val fakta: IngenFakta = IngenFakta
}

data class NedsattArbeidsevnePassAvBarn(
    override val vurderinger: VurderingNedsattArbeidsevne,
) : MålgruppePassAvBarn {
    override val type: MålgruppePassAvBarnType = MålgruppePassAvBarnType.NEDSATT_ARBEIDSEVNE_TILSYN_BARN
    override val fakta: IngenFakta = IngenFakta
}

data class OmstillingsstønadPassAvBarn(
    override val vurderinger: VurderingOmstillingsstønad,
) : MålgruppePassAvBarn {
    override val type: MålgruppePassAvBarnType = MålgruppePassAvBarnType.OMSTILLINGSSTØNAD_TILSYN_BARN
    override val fakta: IngenFakta = IngenFakta
}

data object OvergangssstønadPassAvBarn : MålgruppePassAvBarn {
    override val type: MålgruppePassAvBarnType = MålgruppePassAvBarnType.OVERGANGSSTØNAD_TILSYN_BARN
    override val vurderinger: VurderingOvergangsstønad = VurderingOvergangsstønad
    override val fakta: IngenFakta = IngenFakta
}

data object IngenMålgruppePassAvBarn : MålgruppePassAvBarn {
    override val type: MålgruppePassAvBarnType = MålgruppePassAvBarnType.INGEN_MÅLGRUPPE_TILSYN_BARN
    override val vurderinger: IngenVurderinger = IngenVurderinger
    override val fakta: IngenFakta = IngenFakta
}

data object SykepengerPassAvBarn : MålgruppePassAvBarn {
    override val type: MålgruppePassAvBarnType = MålgruppePassAvBarnType.SYKEPENGER_100_PROSENT_TILSYN_BARN
    override val vurderinger: IngenVurderinger = IngenVurderinger
    override val fakta: IngenFakta = IngenFakta
}

data class TiltakPassAvBarn(
    override val fakta: FaktaAktivitetPassAvBarn,
    override val vurderinger: VurderingTiltakPassAvBarn,
) : AktivitetPassAvBarn {
    override val type: AktivitetPassAvBarnType = AktivitetPassAvBarnType.TILTAK_TILSYN_BARN
}

data class UtdanningPassAvBarn(
    override val fakta: FaktaAktivitetPassAvBarn,
) : AktivitetPassAvBarn {
    override val type: AktivitetPassAvBarnType = AktivitetPassAvBarnType.UTDANNING_TILSYN_BARN
    override val vurderinger: IngenVurderinger = IngenVurderinger
}

data object IngenAktivitetPassAvBarn : AktivitetPassAvBarn {
    override val type: AktivitetPassAvBarnType = AktivitetPassAvBarnType.INGEN_AKTIVITET_TILSYN_BARN
    override val fakta: Fakta = IngenFakta
    override val vurderinger: Vurderinger = IngenVurderinger
}

data class ReellArbeidsøkerPassAvBarn(
    override val fakta: FaktaAktivitetPassAvBarn,
) : AktivitetPassAvBarn {
    override val type: AktivitetPassAvBarnType = AktivitetPassAvBarnType.REELL_ARBEIDSSØKER_TILSYN_BARN
    override val vurderinger: IngenVurderinger = IngenVurderinger
}

data class VurderingTiltakPassAvBarn(
    override val lønnet: VurderingLønnet,
) : LønnetVurdering

data class FaktaAktivitetPassAvBarn(
    override val aktivitetsdager: Int,
) : Fakta,
    FaktaAktivitetsdager {
    init {
        require(aktivitetsdager in 1..5) { "Aktivitetsdager må være mellom 1 og 5" }
    }
}

sealed interface TypeFaktaOgVurderingPassAvBarn : TypeFaktaOgVurdering

enum class AktivitetPassAvBarnType(
    override val vilkårperiodeType: AktivitetType,
) : TypeAktivitetOgVurdering,
    TypeFaktaOgVurderingPassAvBarn {
    UTDANNING_TILSYN_BARN(AktivitetType.UTDANNING),
    TILTAK_TILSYN_BARN(AktivitetType.TILTAK),
    REELL_ARBEIDSSØKER_TILSYN_BARN(AktivitetType.REELL_ARBEIDSSØKER),
    INGEN_AKTIVITET_TILSYN_BARN(AktivitetType.INGEN_AKTIVITET),
}

enum class MålgruppePassAvBarnType(
    override val vilkårperiodeType: MålgruppeType,
) : TypeMålgruppeOgVurdering,
    TypeFaktaOgVurderingPassAvBarn {
    AAP_TILSYN_BARN(MålgruppeType.AAP),
    OMSTILLINGSSTØNAD_TILSYN_BARN(MålgruppeType.OMSTILLINGSSTØNAD),
    OVERGANGSSTØNAD_TILSYN_BARN(MålgruppeType.OVERGANGSSTØNAD),
    NEDSATT_ARBEIDSEVNE_TILSYN_BARN(MålgruppeType.NEDSATT_ARBEIDSEVNE),
    UFØRETRYGD_TILSYN_BARN(MålgruppeType.UFØRETRYGD),
    SYKEPENGER_100_PROSENT_TILSYN_BARN(MålgruppeType.SYKEPENGER_100_PROSENT),
    INGEN_MÅLGRUPPE_TILSYN_BARN(MålgruppeType.INGEN_MÅLGRUPPE),
}
