package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelstrukturDto

data class RegelstrukturReiseOppstartAvslutningHjemreiseDto(
    val oppstart: RegelstrukturDto,
    val avslutning: RegelstrukturDto,
    val hjemreise: RegelstrukturDto,
)
