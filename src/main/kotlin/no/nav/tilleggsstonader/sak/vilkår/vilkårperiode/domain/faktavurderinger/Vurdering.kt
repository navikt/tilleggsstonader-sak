package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis

data class Vurdering(
    val svar: SvarJaNei?,
    val resultat: ResultatDelvilkårperiode,
) {
    init {
        feilHvis(resultat == ResultatDelvilkårperiode.IKKE_AKTUELT && (svar != null)) {
            "Ugyldig resultat=$resultat når svar=$svar"
        }
    }

    companion object {
        val VURDERING_IMPLISITT_OPPFYLT = Vurdering(SvarJaNei.JA_IMPLISITT, ResultatDelvilkårperiode.OPPFYLT)
    }
}

enum class SvarJaNei {
    JA,
    JA_IMPLISITT,
    NEI,
    ;

    fun harVurdert(): Boolean = this != JA_IMPLISITT
}

enum class ResultatDelvilkårperiode {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_VURDERT,
    IKKE_AKTUELT,
}
