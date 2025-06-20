package no.nav.tilleggsstonader.sak.behandling.vent

import no.nav.tilleggsstonader.sak.behandling.vent.KanTaAvVent.Ja.PåkrevdHandling
import no.nav.tilleggsstonader.sak.behandling.vent.KanTaAvVent.Nei.Årsak

data class KanTaAvVentDto(
    val resultat: KanTaAvVentStatus,
) {
    companion object {
        fun fraDomene(kanTaAvVent: KanTaAvVent): KanTaAvVentDto =
            KanTaAvVentDto(
                resultat =
                    when (kanTaAvVent) {
                        is KanTaAvVent.Ja -> {
                            when (kanTaAvVent.påkrevdHandling) {
                                PåkrevdHandling.BehandlingMåNullstilles -> KanTaAvVentStatus.MÅ_NULLSTILLE_BEHANDLING
                                PåkrevdHandling.Ingen -> KanTaAvVentStatus.OK
                            }
                        }

                        is KanTaAvVent.Nei -> {
                            when (kanTaAvVent.årsak) {
                                Årsak.AnnenAktivBehandlingPåFagsaken -> KanTaAvVentStatus.ANNEN_AKTIV_BEHANDLING_PÅ_FAGSAKEN
                                Årsak.ErAlleredePåVent -> KanTaAvVentStatus.ER_ALLEREDE_PÅ_VENT
                            }
                        }
                    },
            )
    }
}

enum class KanTaAvVentStatus {
    OK,
    MÅ_NULLSTILLE_BEHANDLING,
    ANNEN_AKTIV_BEHANDLING_PÅ_FAGSAKEN,
    ER_ALLEREDE_PÅ_VENT,
}
