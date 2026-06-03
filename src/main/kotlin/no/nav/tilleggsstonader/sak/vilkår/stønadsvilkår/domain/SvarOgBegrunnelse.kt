package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId

data class SvarOgBegrunnelse(
    val svar: SvarId,
    val begrunnelse: String? = null,
)
