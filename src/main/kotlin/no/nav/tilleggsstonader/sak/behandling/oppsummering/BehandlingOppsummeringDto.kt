package no.nav.tilleggsstonader.sak.behandling.oppsummering

import com.fasterxml.jackson.annotation.JsonGetter
import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDagligReiseOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDagligReisePrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDagligReiseUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.TypeVilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetsdagerNullable
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingUtil.takeIfFakta
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaStudienivå
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
    val aktivitetsdager: Int?,
    val studienivå: Studienivå?,
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
        aktivitetsdager =
            this.faktaOgVurdering.fakta
                .takeIfFakta<FaktaAktivitetsdagerNullable>()
                ?.aktivitetsdager,
        studienivå =
            this.faktaOgVurdering.fakta
                .takeIfFakta<FaktaStudienivå>()
                ?.studienivå,
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
    val typeFakta: TypeVilkårFakta?,
)

fun Vilkår.tilOppsummertVilkår(): OppsummertVilkår =
    OppsummertVilkår(
        id = this.id,
        fom = this.fom,
        tom = this.tom,
        resultat = this.resultat,
        utgift = this.utgift,
        typeFakta =
            when (this.fakta) {
                is FaktaDagligReiseOffentligTransport -> TypeVilkårFakta.DAGLIG_REISE_OFFENTLIG_TRANSPORT
                is FaktaDagligReisePrivatBil -> TypeVilkårFakta.DAGLIG_REISE_PRIVAT_BIL
                is FaktaDagligReiseUbestemt -> TypeVilkårFakta.DAGLIG_REISE_UBESTEMT
                null -> null
            },
    )
