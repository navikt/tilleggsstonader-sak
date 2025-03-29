package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.vedtak.domain.FaktiskMålgruppe

enum class LovverketsMålgruppeDvh {
    NEDSATT_ARBEIDSEVNE,
    ENSLIG_FORSØRGER,
    GJENLEVENDE,
    ;

    companion object {
        fun fraDomene(målgruppe: FaktiskMålgruppe) =
            when (målgruppe) {
                FaktiskMålgruppe.AAP,
                FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                FaktiskMålgruppe.UFØRETRYGD,
                -> NEDSATT_ARBEIDSEVNE

                FaktiskMålgruppe.OVERGANGSSTØNAD -> ENSLIG_FORSØRGER

                FaktiskMålgruppe.OMSTILLINGSSTØNAD -> GJENLEVENDE
            }
    }
}
