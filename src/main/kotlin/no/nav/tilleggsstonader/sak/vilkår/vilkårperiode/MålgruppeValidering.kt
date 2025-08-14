package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

object MålgruppeValidering {
    fun validerKanLeggeTilMålgruppeManuelt(
        stønadstype: Stønadstype,
        målgruppeType: MålgruppeType,
    ) {
        val gyldig =
            when (stønadstype) {
                Stønadstype.BARNETILSYN, Stønadstype.LÆREMIDLER, Stønadstype.BOUTGIFTER ->
                    when (målgruppeType) {
                        MålgruppeType.AAP -> true
                        MålgruppeType.DAGPENGER -> false
                        MålgruppeType.NEDSATT_ARBEIDSEVNE -> true
                        MålgruppeType.OMSTILLINGSSTØNAD -> true
                        MålgruppeType.OVERGANGSSTØNAD -> true
                        MålgruppeType.UFØRETRYGD -> true
                        MålgruppeType.SYKEPENGER_100_PROSENT -> true
                        MålgruppeType.INGEN_MÅLGRUPPE -> true
                        MålgruppeType.TILTAKSPENGER -> false
                        MålgruppeType.KVALIFISERINGSSTØNAD -> false
                    }
                Stønadstype.DAGLIG_REISE_TSO ->
                    when (målgruppeType) {
                        MålgruppeType.AAP -> true
                        MålgruppeType.DAGPENGER -> false
                        MålgruppeType.NEDSATT_ARBEIDSEVNE -> true
                        MålgruppeType.OMSTILLINGSSTØNAD -> true
                        MålgruppeType.OVERGANGSSTØNAD -> true
                        MålgruppeType.UFØRETRYGD -> true
                        MålgruppeType.SYKEPENGER_100_PROSENT -> false
                        MålgruppeType.INGEN_MÅLGRUPPE -> true
                        MålgruppeType.TILTAKSPENGER -> false
                        MålgruppeType.KVALIFISERINGSSTØNAD -> false
                    }
                Stønadstype.DAGLIG_REISE_TSR ->
                    when (målgruppeType) {
                        MålgruppeType.INGEN_MÅLGRUPPE -> true
                        MålgruppeType.TILTAKSPENGER -> true
                        MålgruppeType.KVALIFISERINGSSTØNAD -> true
                        MålgruppeType.AAP -> false
                        MålgruppeType.DAGPENGER -> true
                        MålgruppeType.NEDSATT_ARBEIDSEVNE -> false
                        MålgruppeType.OMSTILLINGSSTØNAD -> false
                        MålgruppeType.OVERGANGSSTØNAD -> false
                        MålgruppeType.UFØRETRYGD -> false
                        MålgruppeType.SYKEPENGER_100_PROSENT -> false
                    }
            }
        feilHvisIkke(gyldig) {
            "målgruppe=$målgruppeType er ikke gyldig for $stønadstype"
        }
    }
}
