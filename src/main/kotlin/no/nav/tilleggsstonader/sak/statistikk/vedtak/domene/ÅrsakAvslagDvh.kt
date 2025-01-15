package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag

enum class ÅrsakAvslagDvh {
    INGEN_AKTIVITET,
    IKKE_I_MÅLGRUPPE,
    INGEN_OVERLAPP_AKTIVITET_MÅLGRUPPE,
    MANGELFULL_DOKUMENTASJON,
    HAR_IKKE_UTGIFTER,
    RETT_TIL_UTSTYRSSTIPEND,
    ANNET,
    ;

    data class JsonWrapper(
        val årsaker: List<ÅrsakAvslagDvh>,
    )

    companion object {
        fun fraDomene(årsaker: List<ÅrsakAvslag>?): JsonWrapper? {
            return årsaker?.let {
                JsonWrapper(
                    årsaker.map { typeFraDomene(it) },
                )
            }
        }

        private fun typeFraDomene(årsak: ÅrsakAvslag) = when (årsak) {
            ÅrsakAvslag.INGEN_AKTIVITET -> INGEN_AKTIVITET
            ÅrsakAvslag.IKKE_I_MÅLGRUPPE -> IKKE_I_MÅLGRUPPE
            ÅrsakAvslag.INGEN_OVERLAPP_AKTIVITET_MÅLGRUPPE -> INGEN_OVERLAPP_AKTIVITET_MÅLGRUPPE
            ÅrsakAvslag.MANGELFULL_DOKUMENTASJON -> MANGELFULL_DOKUMENTASJON
            ÅrsakAvslag.HAR_IKKE_UTGIFTER -> HAR_IKKE_UTGIFTER
            ÅrsakAvslag.RETT_TIL_UTSTYRSSTIPEND -> RETT_TIL_UTSTYRSSTIPEND
            ÅrsakAvslag.ANNET -> ANNET
        }
    }
}