package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto

import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import java.time.LocalDate

data class VilkårDagligReiseDto(
    val id: VilkårId,
    val fom: LocalDate,
    val tom: LocalDate,
    val adresse: String? = null,
    val resultat: Vilkårsresultat,
    val status: VilkårStatus?,
    val delvilkårsett: List<DelvilkårDto>,
    val fakta: FaktaDagligReiseDto?,
    val slettetKommentar: String? = null,
)
