package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain

import no.nav.tilleggsstonader.libs.feil.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei

data class NødvendigUtgift(
    val erUtgiftenNødvendig: SvarJaNei,
    val begrunnelse: String?,
) {
    init {
        if (erUtgiftenNødvendig == SvarJaNei.NEI) {
            brukerfeilHvisIkke(!begrunnelse.isNullOrBlank()) {
                "Begrunnelse må fylles ut når utgift ikke vurderes som nødvendig"
            }
        }
    }
}
