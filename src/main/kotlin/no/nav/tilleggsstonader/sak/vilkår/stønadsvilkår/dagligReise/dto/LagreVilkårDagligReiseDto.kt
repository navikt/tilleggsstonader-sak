package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.LagreDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.SvarOgBegrunnelse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.LagreVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import java.time.LocalDate

data class LagreDagligReiseDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val adresse: String?,
    val svar: Map<RegelId, SvarOgBegrunnelseDto>,
    val fakta: FaktaDagligReiseDto? = null,
) : LagreVilkår {
    fun tilDomain() =
        LagreDagligReise(
            fom = fom,
            tom = tom,
            svar = svar.mapValues { it.value.tilDomain() },
            fakta = fakta?.mapTilFakta(adresse),
        )
}

data class SvarOgBegrunnelseDto(
    val svar: SvarId,
    val begrunnelse: String? = null,
) {
    fun tilDomain() =
        SvarOgBegrunnelse(
            svar = svar,
            begrunnelse = begrunnelse,
        )
}
