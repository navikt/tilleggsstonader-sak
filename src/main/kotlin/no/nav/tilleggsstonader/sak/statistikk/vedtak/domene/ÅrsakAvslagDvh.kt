package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag

enum class ÅrsakAvslagDvh {
    INGEN_AKTIVITET,
    IKKE_I_MÅLGRUPPE,
    INGEN_OVERLAPP_AKTIVITET_MÅLGRUPPE,
    MANGELFULL_DOKUMENTASJON,
    HAR_IKKE_UTGIFTER,
    RETT_TIL_UTSTYRSSTIPEND,
    HAR_IKKE_MERUTGIFTER,
    RETT_TIL_BOSTØTTE,
    REISEAVSTAND_UNDER_6_KM,
    LØNN_I_TILTAK_ELLER_ORDINAR_LØNN,
    IKKE_I_TILTAK,
    ANNET,
    ;

    data class JsonWrapper(
        val årsaker: List<ÅrsakAvslagDvh>,
    )

    companion object {
        fun fraDomene(årsaker: List<ÅrsakAvslag>?): JsonWrapper? =
            årsaker?.let {
                JsonWrapper(
                    årsaker.map { typeFraDomene(it) },
                )
            }

        private fun typeFraDomene(årsak: ÅrsakAvslag) =
            when (årsak) {
                ÅrsakAvslag.INGEN_AKTIVITET -> INGEN_AKTIVITET
                ÅrsakAvslag.IKKE_I_MÅLGRUPPE -> IKKE_I_MÅLGRUPPE
                ÅrsakAvslag.INGEN_OVERLAPP_AKTIVITET_MÅLGRUPPE -> INGEN_OVERLAPP_AKTIVITET_MÅLGRUPPE
                ÅrsakAvslag.MANGELFULL_DOKUMENTASJON -> MANGELFULL_DOKUMENTASJON
                ÅrsakAvslag.HAR_IKKE_UTGIFTER -> HAR_IKKE_UTGIFTER
                ÅrsakAvslag.RETT_TIL_UTSTYRSSTIPEND -> RETT_TIL_UTSTYRSSTIPEND
                ÅrsakAvslag.HAR_IKKE_MERUTGIFTER -> HAR_IKKE_MERUTGIFTER
                ÅrsakAvslag.RETT_TIL_BOSTØTTE -> RETT_TIL_BOSTØTTE
                ÅrsakAvslag.REISEAVSTAND_UNDER_6_KM -> REISEAVSTAND_UNDER_6_KM
                ÅrsakAvslag.IKKE_I_TILTAK -> IKKE_I_TILTAK
                ÅrsakAvslag.IKKE_RETT_TIL_YTELSE -> IKKE_I_MÅLGRUPPE
                ÅrsakAvslag.LØNN_I_TILTAK_ELLER_ORDINAR_LØNN -> LØNN_I_TILTAK_ELLER_ORDINAR_LØNN
                ÅrsakAvslag.ANNET -> ANNET
            }
    }
}
