package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.LagreVilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.LagreVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import java.time.LocalDate

data class LagreVilkårDagligReiseDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val adresse: String,
    val reiseId: ReiseId,
    val svar: Map<RegelId, SvarOgBegrunnelseDto>,
    val fakta: FaktaDagligReiseDto,
) : LagreVilkår {
    fun tilDomain() =
        LagreVilkårDagligReise(
            fom = fom,
            tom = tom,
            svar = svar.mapValues { it.value.tilDomain() },
            fakta = fakta.mapTilFakta(reiseId = reiseId, adresse = adresse),
        )
}
