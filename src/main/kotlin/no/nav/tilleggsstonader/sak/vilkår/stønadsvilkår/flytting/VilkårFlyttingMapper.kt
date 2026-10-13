package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting

import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FlyttingVilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.FaktaFlyttingMapper.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.domain.VilkårFlytting
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.VilkårFlyttingDto

object VilkårFlyttingMapper {
    fun Vilkår.mapTilVilkårFlytting(): VilkårFlytting {
        if (type != VilkårType.FLYTTING) feil("Ugyldig vilkårstype for flytting")
        val flyttefakta = fakta as? FlyttingVilkårFakta ?: feil("Fakta mangler for flytting")
        return VilkårFlytting(
            id = id,
            behandlingId = behandlingId,
            fom = fom ?: feil("Forventer at fom er satt for flytting"),
            tom = tom ?: feil("Forventer at tom er satt for flytting"),
            resultat = resultat,
            status = status,
            delvilkårsett = delvilkårsett,
            fakta = flyttefakta,
            slettetKommentar = slettetKommentar,
            opphavsvilkår = opphavsvilkår,
        )
    }

    fun VilkårFlytting.mapTilVilkår(): Vilkår =
        Vilkår(
            id = id,
            behandlingId = behandlingId,
            resultat = resultat,
            status = status,
            type = VilkårType.FLYTTING,
            fom = fom,
            tom = tom,
            erFremtidigUtgift = false,
            delvilkårwrapper = DelvilkårWrapper(delvilkårsett),
            opphavsvilkår = opphavsvilkår,
            gitVersjon = Applikasjonsversjon.versjon,
            fakta = fakta,
            slettetKommentar = slettetKommentar,
        )

    fun VilkårFlytting.tilVilkårFlyttingDto(): VilkårFlyttingDto =
        VilkårFlyttingDto(
            id = id,
            fom = fom,
            tom = tom,
            resultat = resultat,
            status = status,
            delvilkårsett = delvilkårsett.map { it.tilDto() },
            fakta = fakta.tilDto(),
            slettetKommentar = slettetKommentar,
        )
}
