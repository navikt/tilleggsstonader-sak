package no.nav.tilleggsstonader.sak.infrastruktur.config

import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.TilgangInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val tilgangInterceptor: TilgangInterceptor,
) : WebMvcConfigurer {
    private val excludePatterns =
        listOf(
            "/api/task/**",
            "/api/v2/task/**",
            "/internal/**",
        )

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(tilgangInterceptor).excludePathPatterns(excludePatterns)
        super.addInterceptors(registry)
    }
}
