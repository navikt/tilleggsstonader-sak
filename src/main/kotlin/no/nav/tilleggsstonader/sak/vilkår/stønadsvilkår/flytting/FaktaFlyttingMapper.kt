package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DokumenterteUtgifter
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlytteSelv
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttebyrå
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttingUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FlyttebyråTilbud
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FlyttingVilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.NødvendigUtgift
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.NødvendigUtgiftDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.DokumenterteUtgifter
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FaktaFlytteSelvDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FaktaFlyttebyråDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FaktaFlyttingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FaktaFlyttingUbestemtDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FlyttebyråTilbudDto

object FaktaFlyttingMapper {
    fun FaktaFlyttingDto.tilDomene(): FlyttingVilkårFakta =
        when (this) {
            is FaktaFlyttebyråDto ->
                FaktaFlyttebyrå(
                    tilbud = tilbud.map { FlyttebyråTilbud(navn = it.navn, pris = it.pris) },
                    adresse = adresse,
                    dokumenterteUtgifter = erUtgifterDokumentert.tilDomene(),
                    nødvendigUtgift = nødvendigUtgift.tilDomene(),
                )

            is FaktaFlytteSelvDto ->
                FaktaFlytteSelv(
                    dokumenterteUtgifter = erUtgifterDokumentert.tilDomene(),
                    avstandEnVei = avstandEnVei,
                    henger = henger,
                    leiebil = leiebil,
                    bompenger = bompenger,
                    ferge = ferge,
                    parkering = parkering,
                    annet = annet,
                    adresse = adresse,
                    nødvendigUtgift = nødvendigUtgift.tilDomene(),
                )

            is FaktaFlyttingUbestemtDto -> FaktaFlyttingUbestemt(adresse = adresse)
        }

    fun FlyttingVilkårFakta.tilDto(): FaktaFlyttingDto =
        when (this) {
            is FaktaFlyttebyrå ->
                FaktaFlyttebyråDto(
                    tilbud = tilbud.map { FlyttebyråTilbudDto(navn = it.navn, pris = it.pris) },
                    adresse = adresse,
                    erUtgifterDokumentert = dokumenterteUtgifter.tilDto(),
                    nødvendigUtgift = nødvendigUtgift.tilDto(),
                )

            is FaktaFlytteSelv ->
                FaktaFlytteSelvDto(
                    erUtgifterDokumentert = dokumenterteUtgifter.tilDto(),
                    avstandEnVei = avstandEnVei,
                    henger = henger,
                    leiebil = leiebil,
                    bompenger = bompenger,
                    ferge = ferge,
                    parkering = parkering,
                    annet = annet,
                    adresse = adresse,
                    nødvendigUtgift = nødvendigUtgift.tilDto(),
                )

            is FaktaFlyttingUbestemt -> FaktaFlyttingUbestemtDto(adresse = adresse)
        }

    private fun NødvendigUtgift.tilDto() =
        NødvendigUtgiftDto(
            erUtgiftenNødvendig = erUtgiftenNødvendig,
            begrunnelse = begrunnelse,
        )

    private fun no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.DokumenterteUtgifter.tilDomene() =
        DokumenterteUtgifter(
            erUtgifterDokumentert = erUtgifterDokumentert,
            begrunnelse = begrunnelse?.trim()?.takeIf { it.isNotEmpty() },
        )

    private fun DokumenterteUtgifter.tilDto() =
        no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.DokumenterteUtgifter(
            erUtgifterDokumentert = erUtgifterDokumentert,
            begrunnelse = begrunnelse,
        )
}
