package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import java.time.LocalDate

data class LagreDagligReise(
    val fom: LocalDate,
    val tom: LocalDate,
    val svar: Map<RegelId, SvarOgBegrunnelse>,
    val fakta: FaktaDagligReise? = null,
)

data class SvarOgBegrunnelse(
    val svar: SvarId,
    val begrunnelse: String? = null,
)
