package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(
    JsonSubTypes.Type(DelvilkårMålgruppe::class, name = "MÅLGRUPPE"),
    JsonSubTypes.Type(DelvilkårAktivitet::class, name = "AKTIVITET"),
)
sealed class DelvilkårVilkårperiode {
    data class Vurdering(
        val svar: SvarJaNei?,
        val resultat: ResultatDelvilkårperiode,
    ) {
        init {
            feilHvis(resultat == ResultatDelvilkårperiode.IKKE_AKTUELT && (svar != null)) {
                "Ugyldig resultat=$resultat når svar=$svar"
            }
        }
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
