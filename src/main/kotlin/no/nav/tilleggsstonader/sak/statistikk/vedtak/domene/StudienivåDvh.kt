package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå

enum class StudienivåDvh {
    VIDEREGÅENDE,
    HØYERE_UTDANNING,
    ;

    companion object {
        fun fraDomene(studienivå: Studienivå) =
            when (studienivå) {
                Studienivå.VIDEREGÅENDE -> VIDEREGÅENDE
                Studienivå.HØYERE_UTDANNING -> HØYERE_UTDANNING
            }
    }
}
