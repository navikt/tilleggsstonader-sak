package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.LagreVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.LagreVilkårReiseTilSamling
import java.time.LocalDate

data class LagreVilkårReiseTilSamlingDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val adresse: String,
    val reiseId: ReiseId,
    val svar: Map<RegelId, SvarOgBegrunnelseDto>,
    val fakta: FaktaReiseTilSamlingDto,
) : LagreVilkår {
    fun tilDomain() =
        LagreVilkårReiseTilSamling(
            fom = fom,
            tom = tom,
            svar = svar.mapValues { it.value.tilDomain() },
            fakta = fakta.mapTilFakta(reiseId = reiseId, adresse = adresse),
        )
}
