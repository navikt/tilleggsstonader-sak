package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain

import no.nav.tilleggsstonader.libs.feil.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei

data class DokumenterteUtgifter(
    val erUtgifterDokumentert: SvarJaNei,
    val begrunnelse: String?,
) {
    init {
        if (erUtgifterDokumentert == SvarJaNei.JA) {
            brukerfeilHvisIkke(!begrunnelse.isNullOrBlank()) {
                "Begrunnelse må fylles ut når utgifter er dokumentert og skal utbetales"
            }
        }
    }
}
