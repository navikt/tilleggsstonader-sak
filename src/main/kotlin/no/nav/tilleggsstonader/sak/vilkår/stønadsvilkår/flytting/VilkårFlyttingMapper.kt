package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting

import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FlyttingVilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.FaktaFlyttingMapper.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.VilkårFlyttingDto

object VilkårFlyttingMapper {
    fun Vilkår.tilVilkårFlyttingDto(): VilkårFlyttingDto {
        if (type != VilkårType.FLYTTING) feil("Ugyldig vilkårstype for flytting")
        val flyttefakta = fakta as? FlyttingVilkårFakta ?: feil("Fakta mangler for flytting")
        return VilkårFlyttingDto(
            id = id,
            fom = fom ?: error("Forventer at fom er satt for flytting"),
            tom = tom ?: error("Forventer at tom er satt for flytting"),
            resultat = resultat,
            status = status,
            delvilkårsett = delvilkårsett.map { it.tilDto() },
            fakta = flyttefakta.tilDto(),
            slettetKommentar = slettetKommentar,
        )
    }
}
