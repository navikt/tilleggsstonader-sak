package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto

import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårGlobalId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import java.time.LocalDate

data class VilkårMidlertidigBostedDto(
    val id: VilkårId,
    val globalId: VilkårGlobalId?,
    val fom: LocalDate,
    val tom: LocalDate,
    val aktivitetId: VilkårperiodeGlobalId,
    val resultat: Vilkårsresultat,
    val status: VilkårStatus?,
    val delvilkårsett: List<DelvilkårDto>,
    val fraAdresse: String,
    val tilAdresse: String,
    val slettetKommentar: String? = null,
) : VilkårROAHDto
