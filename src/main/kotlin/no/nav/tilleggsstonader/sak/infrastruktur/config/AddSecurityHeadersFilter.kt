package no.nav.tilleggsstonader.sak.infrastruktur.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class AddSecurityHeadersFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        response.setHeader("X-Content-Type-Options", "nosniff")
        response.setHeader("Cache-Control", "private, max-age=0, no-cache, no-store")

        filterChain.doFilter(request, response)
    }
}
