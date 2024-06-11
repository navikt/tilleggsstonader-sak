package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

object MålgruppeValidering {

    fun validerKanLeggeTilMålgruppeManuelt(stønadstype: Stønadstype, målgruppeType: MålgruppeType) {
        val gyldig = when (stønadstype) {
            Stønadstype.BARNETILSYN -> when (målgruppeType) {
                MålgruppeType.AAP -> true
                MålgruppeType.DAGPENGER -> false
                MålgruppeType.NEDSATT_ARBEIDSEVNE -> true
                MålgruppeType.OMSTILLINGSSTØNAD -> true
                MålgruppeType.OVERGANGSSTØNAD -> true
                MålgruppeType.UFØRETRYGD -> true
                MålgruppeType.SYKEPENGER_100_PROSENT -> true
                MålgruppeType.INGEN_MÅLGRUPPE -> true
            }
        }
        feilHvisIkke(gyldig) {
            "målgruppe=$målgruppeType er ikke gyldig for $stønadstype"
        }
    }
}
