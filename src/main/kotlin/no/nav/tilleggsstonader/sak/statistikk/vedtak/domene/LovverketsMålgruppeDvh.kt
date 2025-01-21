package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

enum class LovverketsMålgruppeDvh {
    NEDSATT_ARBEIDSEVNE,
    ENSLIG_FORSØRGER,
    GJENLEVENDE,
    INGEN_MÅLGRUPPE,
    ;

    companion object {
        fun fraDomene(målgruppe: MålgruppeType) = when (målgruppe) {
            MålgruppeType.AAP,
            MålgruppeType.NEDSATT_ARBEIDSEVNE,
            MålgruppeType.UFØRETRYGD,
            -> NEDSATT_ARBEIDSEVNE

            MålgruppeType.OVERGANGSSTØNAD -> ENSLIG_FORSØRGER

            MålgruppeType.OMSTILLINGSSTØNAD -> GJENLEVENDE

            MålgruppeType.DAGPENGER,
            MålgruppeType.SYKEPENGER_100_PROSENT,
            MålgruppeType.INGEN_MÅLGRUPPE,
            -> INGEN_MÅLGRUPPE
        }
    }
}
