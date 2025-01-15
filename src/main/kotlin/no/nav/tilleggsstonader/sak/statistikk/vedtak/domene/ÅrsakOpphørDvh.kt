package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør

enum class ÅrsakOpphørDvh {
    ENDRING_AKTIVITET,
    ENDRING_MÅLGRUPPE,
    ENDRING_UTGIFTER,
    ANNET,
    ;

    data class JsonWrapper(
        val årsaker: List<ÅrsakOpphørDvh>,
    )

    companion object {
        fun fraDomene(årsaker: List<ÅrsakOpphør>?): JsonWrapper? {
            return årsaker?.let {
                JsonWrapper(
                    årsaker.map { typeFraDomene(it) },
                )
            }
        }

        private fun typeFraDomene(årsak: ÅrsakOpphør) = when (årsak) {
            ÅrsakOpphør.ENDRING_AKTIVITET -> ENDRING_AKTIVITET
            ÅrsakOpphør.ENDRING_MÅLGRUPPE -> ENDRING_MÅLGRUPPE
            ÅrsakOpphør.ENDRING_UTGIFTER -> ENDRING_UTGIFTER
            ÅrsakOpphør.ANNET -> ANNET
        }
    }
}