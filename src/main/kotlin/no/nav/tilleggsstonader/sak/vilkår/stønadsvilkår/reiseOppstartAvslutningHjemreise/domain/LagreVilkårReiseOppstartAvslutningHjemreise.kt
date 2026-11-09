package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.SvarOgBegrunnelse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårGlobalId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import java.time.LocalDate

data class LagreVilkårReiseOppstartAvslutningHjemreise(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val reiseId: ReiseId,
    override val aktivitetId: VilkårperiodeGlobalId,
    val typeReiseformål: TypeReiseformål,
    override val svar: Map<RegelId, SvarOgBegrunnelse>,
    val fakta: FaktaReiseOppstartAvslutningHjemreise,
    val parentVilkår: VilkårGlobalId? = null,
) : LagreVilkårROAH {
    override val vilkårType get() = typeReiseformål.tilVilkårType()
}
