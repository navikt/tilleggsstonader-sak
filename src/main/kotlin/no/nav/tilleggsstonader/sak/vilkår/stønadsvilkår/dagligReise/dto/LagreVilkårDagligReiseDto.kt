package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.LagreDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.SvarOgBegrunnelse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import java.time.LocalDate

data class LagreDagligReiseDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val svar: Map<RegelId, SvarOgBegrunnelseDto>,
    val fakta: FaktaDagligReiseDto? = null,
) {
    fun tilDomain() =
        LagreDagligReise(
            fom = fom,
            tom = tom,
            svar = svar.mapValues { it.value.tilDomain() },
            fakta = fakta?.mapTilFakta(),
        )
}

data class SvarOgBegrunnelseDto(
    val svarId: SvarId,
    val begrunnelse: String? = null,
) {
    fun tilDomain() =
        SvarOgBegrunnelse(
            svarId = svarId,
            begrunnelse = begrunnelse,
        )
}
