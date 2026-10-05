package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.domain

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FlyttingVilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.SvarOgBegrunnelse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import java.time.LocalDate

data class LagreVilkårFlytting(
    val fom: LocalDate,
    val tom: LocalDate,
    val svar: Map<RegelId, SvarOgBegrunnelse>,
    val fakta: FlyttingVilkårFakta,
)
