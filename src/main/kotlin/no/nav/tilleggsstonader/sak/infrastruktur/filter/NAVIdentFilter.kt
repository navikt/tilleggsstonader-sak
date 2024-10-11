package no.nav.tilleggsstonader.sak.infrastruktur.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext.SYSTEM_FORKORTELSE
import org.slf4j.MDC

private const val NAV_IDENT = "navIdent"

/**
 * Legger til navIdent fra token i filter for å kunne se hvem som ev. fått feil sånn at vi kan kontakte saksbehandleren
 */
class NAVIdentFilter : HttpFilter() {

    override fun doFilter(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val saksbehandler = SikkerhetContext.hentSaksbehandlerEllerSystembruker()
        if (saksbehandler != SYSTEM_FORKORTELSE) {
            MDC.put(NAV_IDENT, saksbehandler)
        }
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(NAV_IDENT)
        }
    }
}
