package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

enum class FaktiskMålgruppeDvh {
    NEDSATT_ARBEIDSEVNE,
    ENSLIG_FORSØRGER,
    GJENLEVENDE,
    INGEN_MÅLGRUPPE,
    ;

    companion object {
        fun fraDomene(målgruppe: MålgruppeType) = when (målgruppe) {

            MålgruppeType.OMSTILLINGSSTØNAD -> GJENLEVENDE

            MålgruppeType.OVERGANGSSTØNAD -> ENSLIG_FORSØRGER

            MålgruppeType.AAP,
            MålgruppeType.NEDSATT_ARBEIDSEVNE,
            MålgruppeType.UFØRETRYGD
                -> NEDSATT_ARBEIDSEVNE

            MålgruppeType.DAGPENGER,
            MålgruppeType.SYKEPENGER_100_PROSENT,
            MålgruppeType.INGEN_MÅLGRUPPE
                -> INGEN_MÅLGRUPPE
        }
    }
}