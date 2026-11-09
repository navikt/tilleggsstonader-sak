package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.LagreVilkårMidlertidigBosted
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import java.time.LocalDate

data class LagreVilkårMidlertidigBostedDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val aktivitetId: VilkårperiodeGlobalId,
    val svar: Map<RegelId, SvarOgBegrunnelseDto>,
    val fraAdresse: String,
    val tilAdresse: String,
) : LagreVilkårROAHDto {
    override fun tilDomene() =
        LagreVilkårMidlertidigBosted(
            fom = fom,
            tom = tom,
            aktivitetId = aktivitetId,
            svar = svar.mapValues { it.value.tilDomene() },
            fraAdresse = fraAdresse,
            tilAdresse = tilAdresse,
        )
}
