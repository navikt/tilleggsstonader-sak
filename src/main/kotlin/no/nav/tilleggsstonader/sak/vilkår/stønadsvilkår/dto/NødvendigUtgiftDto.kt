package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto

import no.nav.tilleggsstonader.libs.feil.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.NødvendigUtgift
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei

data class NødvendigUtgiftDto(
    val erUtgiftenNødvendig: SvarJaNei,
    val begrunnelse: String?,
) {
    fun tilDomene(): NødvendigUtgift {
        feilHvis(erUtgiftenNødvendig != SvarJaNei.JA && erUtgiftenNødvendig != SvarJaNei.NEI) {
            "erUtgiftenNødvendig må være JA eller NEI"
        }

        return NødvendigUtgift(
            erUtgiftenNødvendig = erUtgiftenNødvendig,
            begrunnelse = begrunnelse?.ifBlank { null },
        )
    }
}
