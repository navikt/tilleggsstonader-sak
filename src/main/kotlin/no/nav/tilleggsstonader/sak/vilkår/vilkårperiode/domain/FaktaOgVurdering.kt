package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import java.time.LocalDate

sealed interface FaktaOgVurdering : Periode<LocalDate> {
    override val fom: LocalDate
    override val tom: LocalDate
    val fakta: Fakta
    val vurderinger: Vurderinger
    val begrunnelse: String?
}

data class TomFaktaOgVurdering(
    override val fom: LocalDate,
    override val tom: LocalDate,
) : FaktaOgVurdering {
    override val fakta: TomFakta = TomFakta
    override val vurderinger: TomVurdering = TomVurdering
    override val begrunnelse: String? = null
}

data object TomFakta : Fakta

sealed interface Vurderinger

sealed interface Fakta
sealed interface FaktaAktivitetsdager {
    val aktivitetsdager: Int
}

data class TiltakTilsynBarn(
    override val fakta: FaktaAktivitetTilsynBarn,
    override val vurderinger: TiltakTilsynBarnVurdering,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val begrunnelse: String?,
) : FaktaOgVurdering

data class MålgruppeTilsynBarn(
    override val fakta: TomFakta = TomFakta,
    override val vurderinger: MålgruppeVurderingerTilsynBarn,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val begrunnelse: String?,
) : FaktaOgVurdering

data class MålgruppeVurderingerTilsynBarn(
    val medlemskap: DelvilkårVilkårperiode.Vurdering,
    val dekketAvAnnetRegelverk: DelvilkårVilkårperiode.Vurdering,
) : Vurderinger

data class UtdanningTilsynBarn(
    override val fakta: FaktaAktivitetTilsynBarn,
    override val vurderinger: TomVurdering = TomVurdering,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val begrunnelse: String?,
) : FaktaOgVurdering

data class ReellArbeidsøkerTilsynBarn(
    override val fakta: FaktaAktivitetTilsynBarn,
    override val vurderinger: TomVurdering = TomVurdering,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val begrunnelse: String?,
) : FaktaOgVurdering

data object TomVurdering : Vurderinger

data class TiltakTilsynBarnVurdering(val lønnet: DelvilkårVilkårperiode.Vurdering) : Vurderinger

data class FaktaAktivitetTilsynBarn(
    override val aktivitetsdager: Int,
) : FaktaAktivitetsdager, Fakta
