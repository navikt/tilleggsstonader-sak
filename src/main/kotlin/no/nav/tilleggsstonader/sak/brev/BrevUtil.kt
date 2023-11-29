package no.nav.tilleggsstonader.sak.brev

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke

object BrevUtil {
    const val SAKSBEHANDLER_SIGNATUR_PLACEHOLDER = "SAKSBEHANDLER_SIGNATUR"
    const val BESLUTTER_SIGNATUR_PLACEHOLDER = "BESLUTTER_SIGNATUR"
    const val BESLUTTER_VEDTAKSDATO_PLACEHOLDER = "BESLUTTER_VEDTAKSDATO"

    fun settInnSaksbehandlerSignatur(html: String, saksbehandlerSignatur: String): String {
        feilHvisIkke(html.contains(SAKSBEHANDLER_SIGNATUR_PLACEHOLDER)) {
            "Brev-HTML mangler placeholder for saksbehandlersignatur"
        }

        return html.replace(SAKSBEHANDLER_SIGNATUR_PLACEHOLDER, saksbehandlerSignatur)
    }
}
