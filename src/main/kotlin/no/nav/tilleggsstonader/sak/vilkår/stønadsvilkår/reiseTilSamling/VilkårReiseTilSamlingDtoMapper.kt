package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.VilkårReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.FaktaReiseTilSamlingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.VilkårReiseTilSamlingDto

object VilkårReiseTilSamlingDtoMapper {
    fun VilkårReiseTilSamling.tilDto() =
        VilkårReiseTilSamlingDto(
            id = this.id,
            fom = this.fom,
            tom = this.tom,
            adresse = fakta.adresse,
            reiseId = fakta.reiseId,
            resultat = this.resultat,
            status = this.status,
            delvilkårsett = this.delvilkårsett.map { it.tilDto() },
            fakta = this.fakta.tilDto(),
            slettetKommentar = this.slettetKommentar,
        )

    private fun FaktaReiseTilSamling.tilDto() =
        FaktaReiseTilSamlingDto(
            utgifterOffentligTransport = utgifterOffentligTransport,
            reiseavstand = reiseavstand,
        )
}
