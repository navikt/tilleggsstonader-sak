package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import java.time.LocalDate

data class LagreDagligReiseDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val svar: Map<RegelId, SvarOgBegrunnelseDto>,
    val fakta: FaktaDagligReiseDto? = null,
)

data class SvarOgBegrunnelseDto(
    val svarId: SvarId,
    val begrunnelse: String? = null,
)
