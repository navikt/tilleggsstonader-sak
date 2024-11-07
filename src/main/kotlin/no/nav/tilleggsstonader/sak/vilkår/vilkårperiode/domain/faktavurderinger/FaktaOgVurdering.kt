package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode
import java.time.LocalDate

sealed interface FaktaOgVurdering : Periode<LocalDate>, FaktaOgVurderingJson {
    val type: TypeFaktaOgVurdering
    override val fom: LocalDate
    override val tom: LocalDate
    val fakta: Fakta
    val vurderinger: Vurderinger
    val begrunnelse: String?
}

data class TomFaktaOgVurdering(
    override val type: TypeFaktaOgVurdering,
    override val fom: LocalDate,
    override val tom: LocalDate,
) : FaktaOgVurdering {
    override val fakta: TomFakta = TomFakta
    override val vurderinger: TomVurdering = TomVurdering
    override val begrunnelse: String? = null
}

data class MålgruppeVurderinger(
    override val medlemskap: DelvilkårVilkårperiode.Vurdering,
    override val dekketAvAnnetRegelverk: DelvilkårVilkårperiode.Vurdering,
) : MedlemskapVurdering, DekketAvAnnetRegelverkVurdering

data object TomFakta : Fakta

sealed interface Vurderinger

sealed interface LønnetVurdering : Vurderinger {
    val lønnet: DelvilkårVilkårperiode.Vurdering
}

sealed interface MedlemskapVurdering : Vurderinger {
    val medlemskap: DelvilkårVilkårperiode.Vurdering
}

sealed interface DekketAvAnnetRegelverkVurdering : Vurderinger {
    val dekketAvAnnetRegelverk: DelvilkårVilkårperiode.Vurdering
}

sealed interface Fakta
sealed interface FaktaAktivitetsdager {
    val aktivitetsdager: Int
}

data object TomVurdering : Vurderinger
