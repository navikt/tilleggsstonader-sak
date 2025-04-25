package no.nav.tilleggsstonader.sak.infrastruktur.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpFilter
import org.slf4j.MDC

class BehandlingLogFilter : HttpFilter() {
    override fun doFilter(
        req: ServletRequest?,
        res: ServletResponse?,
        chain: FilterChain?,
    ) {
        try {
            super.doFilter(req, res, chain)
        } finally {
            TypeBehandlingLogging.entries.forEach { MDC.remove(it.key) }
        }
    }
}
