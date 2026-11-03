package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei

data class DokumenterteUtgifterDto(
    val erUtgifterDokumentert: SvarJaNei,
    val begrunnelse: String? = null,
)
