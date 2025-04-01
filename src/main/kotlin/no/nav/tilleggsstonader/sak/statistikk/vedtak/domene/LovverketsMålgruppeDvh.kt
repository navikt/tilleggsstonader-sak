package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe

enum class LovverketsMålgruppeDvh {
    NEDSATT_ARBEIDSEVNE,
    ENSLIG_FORSØRGER,
    GJENLEVENDE,
    ;

    companion object {
        fun fraDomene(målgruppe: FaktiskMålgruppe) =
            when (målgruppe) {
                FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE -> NEDSATT_ARBEIDSEVNE
                FaktiskMålgruppe.ENSLIG_FORSØRGER -> ENSLIG_FORSØRGER
                FaktiskMålgruppe.GJENLEVENDE -> GJENLEVENDE
            }
    }
}
