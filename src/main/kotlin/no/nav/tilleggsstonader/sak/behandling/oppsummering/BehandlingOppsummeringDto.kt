package no.nav.tilleggsstonader.sak.behandling.oppsummering

import com.fasterxml.jackson.annotation.JsonGetter
import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import java.time.LocalDate
import java.util.UUID

data class BehandlingOppsummeringDto(
    val aktiviteter: List<OppsummertVilkårperiode>,
    val målgrupper: List<OppsummertVilkårperiode>,
    val vilkår: List<Stønadsvilkår>,
    val vedtak: OppsummertVedtak?,
) {
    @JsonGetter
    fun finnesDataÅOppsummere(): Boolean =
        aktiviteter.isNotEmpty() ||
            målgrupper.isNotEmpty() ||
            vilkår.isNotEmpty() ||
            vedtak != null
}

data class OppsummertVilkårperiode(
    val id: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val type: VilkårperiodeType,
    val resultat: ResultatVilkårperiode,
) : Periode<LocalDate>,
    KopierPeriode<OppsummertVilkårperiode> {
    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): OppsummertVilkårperiode = this.copy(fom = fom, tom = tom)
}

fun Vilkårperiode.tilOppsummertVilkårperiode(): OppsummertVilkårperiode =
    OppsummertVilkårperiode(
        id = this.id,
        fom = this.fom,
        tom = this.tom,
        type = this.type,
        resultat = this.resultat,
    )

data class Stønadsvilkår(
    val type: VilkårType,
    val barnId: BarnId?,
    val vilkår: List<OppsummertVilkår>,
)

data class OppsummertVilkår(
    val id: VilkårId,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val resultat: Vilkårsresultat,
    val utgift: Int?,
)

fun Vilkår.tilOppsummertVilkår(): OppsummertVilkår =
    OppsummertVilkår(
        id = this.id,
        fom = this.fom,
        tom = this.tom,
        resultat = this.resultat,
        utgift = this.utgift,
    )
