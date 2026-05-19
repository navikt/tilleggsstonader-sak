package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.SvarOgBegrunnelse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId

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
