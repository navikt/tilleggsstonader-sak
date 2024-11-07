package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate

sealed interface FaktaOgVurderingTilsynBarn : FaktaOgVurdering {
    override val type: TypeFaktaOgVurderingTilsynBarn
}

sealed interface AktivitetTilsynBarn : FaktaOgVurderingTilsynBarn {
    override val type: AktivitetTilsynBarnType
}

data class MålgruppeTilsynBarn(
    override val type: MålgruppeTilsynBarnType,
    override val vurderinger: MålgruppeVurderinger,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val begrunnelse: String?,
) : FaktaOgVurderingTilsynBarn {
    override val fakta: TomFakta = TomFakta
}

data class TiltakTilsynBarn(
    override val fakta: FaktaAktivitetTilsynBarn,
    override val vurderinger: VurderingTiltakTilsynBarn,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val begrunnelse: String?,
) : AktivitetTilsynBarn {
    override val type: AktivitetTilsynBarnType = AktivitetTilsynBarnType.TILTAK_TILSYN_BARN
}

data class UtdanningTilsynBarn(
    override val fakta: FaktaAktivitetTilsynBarn,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val begrunnelse: String?,
) : AktivitetTilsynBarn {
    override val type: AktivitetTilsynBarnType = AktivitetTilsynBarnType.UTDANNING_TILSYN_BARN
    override val vurderinger: TomVurdering = TomVurdering
}

data class IngenAktivitetTilsynBarn(
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val begrunnelse: String?,
) : AktivitetTilsynBarn {
    override val type: AktivitetTilsynBarnType = AktivitetTilsynBarnType.INGEN_AKTIVITET_TILSYN_BARN
    override val fakta: Fakta = TomFakta
    override val vurderinger: Vurderinger = TomVurdering
}

data class ReellArbeidsøkerTilsynBarn(
    override val fakta: FaktaAktivitetTilsynBarn,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val begrunnelse: String?,
) : AktivitetTilsynBarn {
    override val type: AktivitetTilsynBarnType = AktivitetTilsynBarnType.REELL_ARBEIDSSØKER_TILSYN_BARN
    override val vurderinger: TomVurdering = TomVurdering
}

data class VurderingTiltakTilsynBarn(
    override val lønnet: DelvilkårVilkårperiode.Vurdering,
) : LønnetVurdering

data class FaktaAktivitetTilsynBarn(
    override val aktivitetsdager: Int,
) : Fakta, FaktaAktivitetsdager

sealed interface TypeFaktaOgVurderingTilsynBarn : TypeFaktaOgVurdering

enum class AktivitetTilsynBarnType(val aktivitetType: AktivitetType) : TypeFaktaOgVurderingTilsynBarn {
    UTDANNING_TILSYN_BARN(AktivitetType.UTDANNING),
    TILTAK_TILSYN_BARN(AktivitetType.TILTAK),
    REELL_ARBEIDSSØKER_TILSYN_BARN(AktivitetType.REELL_ARBEIDSSØKER),
    INGEN_AKTIVITET_TILSYN_BARN(AktivitetType.INGEN_AKTIVITET),
}

enum class MålgruppeTilsynBarnType(val målgruppeType: MålgruppeType) : TypeFaktaOgVurderingTilsynBarn {
    AAP_TILSYN_BARN(MålgruppeType.AAP),
    OMSTILLINGSSTØNAD_TILSYN_BARN(MålgruppeType.OMSTILLINGSSTØNAD),
    OVERGANGSSTØNAD_TILSYN_BARN(MålgruppeType.OVERGANGSSTØNAD),
    NEDSATT_ARBEIDSEVNE_TILSYN_BARN(MålgruppeType.NEDSATT_ARBEIDSEVNE),
    UFØRETRYGD_TILSYN_BARN(MålgruppeType.UFØRETRYGD),
    SYKEPENGER_100_PROSENT_TILSYN_BARN(MålgruppeType.SYKEPENGER_100_PROSENT),
    INGEN_MÅLGRUPPE_TILSYN_BARN(MålgruppeType.INGEN_MÅLGRUPPE),
}
