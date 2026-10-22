package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.SvarOgBegrunnelse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import java.time.LocalDate

data class LagreVilkårReiseOppstartAvslutningHjemreise(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val reiseId: ReiseId,
    val aktivitetId: VilkårperiodeGlobalId,
    val typeReiseformål: TypeReiseformål,
    val svar: Map<RegelId, SvarOgBegrunnelse>,
    val fakta: FaktaReiseOppstartAvslutningHjemreise,
) : Periode<LocalDate>
