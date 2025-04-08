package no.nav.tilleggsstonader.sak.oppfølging.domain

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeUtil.ofType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeFaktaOgVurdering
import java.time.LocalDate

data class OppfølgingMålgruppe(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppe: MålgruppeType,
) : Periode<LocalDate>,
    Mergeable<LocalDate, OppfølgingMålgruppe> {
    constructor(vilkårperiode: VilkårperiodeMålgruppe) :
        this(
            fom = vilkårperiode.fom,
            tom = vilkårperiode.tom,
            målgruppe = vilkårperiode.faktaOgVurdering.type.vilkårperiodeType,
        )

    override fun merge(other: OppfølgingMålgruppe): OppfølgingMålgruppe =
        this.copy(fom = minOf(fom, other.fom), tom = maxOf(tom, other.tom))

    fun skalKontrolleres() =
        when (this.målgruppe) {
            MålgruppeType.AAP,
            MålgruppeType.OMSTILLINGSSTØNAD,
            MålgruppeType.OVERGANGSSTØNAD,
            -> true

            MålgruppeType.DAGPENGER -> error("Håndterer ikke dagpenger ennå")
            MålgruppeType.NEDSATT_ARBEIDSEVNE,
            MålgruppeType.UFØRETRYGD,
            MålgruppeType.SYKEPENGER_100_PROSENT,
            MålgruppeType.INGEN_MÅLGRUPPE,
            -> false
        }

    companion object {
        fun fraVilkårperioder(vilkårperioder: List<Vilkårperiode>): List<OppfølgingMålgruppe> =
            vilkårperioder
                .ofType<MålgruppeFaktaOgVurdering>()
                .map { OppfølgingMålgruppe(it) }
                .filter { it.skalKontrolleres() }
                .groupBy { it.målgruppe }
                .mapValues {
                    it.value
                        .sorted()
                        .mergeSammenhengende { m1, m2 -> m1.overlapperEllerPåfølgesAv(m2) }
                }.values
                .flatten()
    }
}
