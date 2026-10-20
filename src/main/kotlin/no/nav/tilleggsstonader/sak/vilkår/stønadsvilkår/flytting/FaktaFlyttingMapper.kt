package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlytteSelv
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttebyrå
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttingUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FlyttebyråTilbud
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FlyttingVilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FaktaFlytteSelvDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FaktaFlyttebyråDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FaktaFlyttingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FaktaFlyttingUbestemtDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FlyttebyråTilbudDto

object FaktaFlyttingMapper {
    fun FaktaFlyttingDto.tilDomain(): FlyttingVilkårFakta =
        when (this) {
            is FaktaFlyttebyråDto ->
                FaktaFlyttebyrå(
                    tilbud = tilbud.map { FlyttebyråTilbud(navn = it.navn, pris = it.pris) },
                    adresse = adresse,
                    erBetalingDokumentert = erBetalingDokumentert,
                )

            is FaktaFlytteSelvDto ->
                FaktaFlytteSelv(
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
                    tilbud = tilbud.map { FlyttebyråTilbudDto(navn = it.navn, pris = it.pris) },
                    adresse = adresse,
                    erBetalingDokumentert = erBetalingDokumentert,
                )

            is FaktaFlytteSelv ->
                FaktaFlytteSelvDto(
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
