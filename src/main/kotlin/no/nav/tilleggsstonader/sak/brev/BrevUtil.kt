package no.nav.tilleggsstonader.sak.brev

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.util.norskFormat
import java.time.LocalDate

object BrevUtil {
    const val SAKSBEHANDLER_SIGNATUR_PLACEHOLDER = "SAKSBEHANDLER_SIGNATUR"
    const val BESLUTTER_SIGNATUR_PLACEHOLDER = "BESLUTTER_SIGNATUR"
    const val BREVDATO_PLACEHOLDER = "BREVDATO_PLACEHOLDER"

    fun settInnSaksbehandlerSignaturOgDato(html: String, saksbehandlerSignatur: String): String {
        feilHvisIkke(html.contains(SAKSBEHANDLER_SIGNATUR_PLACEHOLDER)) {
            "Brev-HTML mangler placeholder for saksbehandlersignatur"
        }
        feilHvisIkke(html.contains(BREVDATO_PLACEHOLDER)) {
            "Brev-HTML mangler placeholder for saksbehandlersignatur"
        }

        return html
            .replace(SAKSBEHANDLER_SIGNATUR_PLACEHOLDER, saksbehandlerSignatur)
            .replace(BREVDATO_PLACEHOLDER, LocalDate.now().norskFormat())
    }
}
