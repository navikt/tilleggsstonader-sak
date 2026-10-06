package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttebyrå
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttingUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaKjøreSelv
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FlyttebyråTilbud
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FlyttingVilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FaktaFlyttebyråDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FaktaFlyttingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FaktaFlyttingUbestemtDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FaktaKjøreSelvDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FlyttebyråTilbudDto

object FaktaFlyttingMapper {
    fun FaktaFlyttingDto.tilDomain(): FlyttingVilkårFakta =
        when (this) {
            is FaktaFlyttebyråDto ->
                FaktaFlyttebyrå(
                    tilbud1 = FlyttebyråTilbud(navn = tilbud1.navn, pris = tilbud1.pris),
                    tilbud2 = FlyttebyråTilbud(navn = tilbud2.navn, pris = tilbud2.pris),
                    adresse = adresse,
                )
            is FaktaKjøreSelvDto ->
                FaktaKjøreSelv(
                    avstandEnVei = avstandEnVei,
                    henger = henger,
                    bompenger = bompenger,
                    ferge = ferge,
                    parkering = parkering,
                    adresse = adresse,
                )
            is FaktaFlyttingUbestemtDto -> FaktaFlyttingUbestemt(adresse = adresse)
        }

    fun FlyttingVilkårFakta.tilDto(): FaktaFlyttingDto =
        when (this) {
            is FaktaFlyttebyrå ->
                FaktaFlyttebyråDto(
                    tilbud1 = FlyttebyråTilbudDto(navn = tilbud1.navn, pris = tilbud1.pris),
                    tilbud2 = FlyttebyråTilbudDto(navn = tilbud2.navn, pris = tilbud2.pris),
                    adresse = adresse,
                )
            is FaktaKjøreSelv ->
                FaktaKjøreSelvDto(
                    avstandEnVei = avstandEnVei,
                    henger = henger,
                    bompenger = bompenger,
                    ferge = ferge,
                    parkering = parkering,
                    adresse = adresse,
                )
            is FaktaFlyttingUbestemt -> FaktaFlyttingUbestemtDto(adresse = adresse)
        }
}
