package no.nav.tilleggsstonader.sak.infrastruktur.config

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import no.nav.tilleggsstonader.libs.log.filter.LogFilterConfiguration
import org.springframework.boot.SpringBootConfiguration
import org.springframework.context.annotation.Import

@SpringBootConfiguration
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
// @EnableOAuth2Client(cacheEnabled = true)
@Import(
    // RestTemplateConfiguration::class,
    LogFilterConfiguration::class,
)
class ApplicationConfig
