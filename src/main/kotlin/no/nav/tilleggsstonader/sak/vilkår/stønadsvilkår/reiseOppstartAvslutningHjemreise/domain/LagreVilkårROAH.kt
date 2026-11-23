package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.SvarOgBegrunnelse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårGlobalId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import java.time.LocalDate

sealed interface LagreVilkårROAH : Periode<LocalDate> {
    val aktivitetId: VilkårperiodeGlobalId
    val svar: Map<RegelId, SvarOgBegrunnelse>
    val vilkårType: VilkårType
}

data class LagreVilkårMidlertidigBosted(
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val aktivitetId: VilkårperiodeGlobalId,
    override val svar: Map<RegelId, SvarOgBegrunnelse>,
    val fakta: FaktaMidlertidigBosted,
) : LagreVilkårROAH {
    override val vilkårType = VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE
}

data class LagreVilkårROAHReise(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val reiseId: ReiseId,
    override val aktivitetId: VilkårperiodeGlobalId,
    val typeReiseformål: TypeReiseformål,
    override val svar: Map<RegelId, SvarOgBegrunnelse>,
    val fakta: FaktaReiseROAH,
    val parentVilkår: VilkårGlobalId,
) : LagreVilkårROAH {
    override val vilkårType get() = typeReiseformål.tilVilkårType()
}
