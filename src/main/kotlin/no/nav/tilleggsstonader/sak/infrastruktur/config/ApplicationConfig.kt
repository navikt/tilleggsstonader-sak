package no.nav.tilleggsstonader.sak.infrastruktur.config

import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import no.nav.tilleggsstonader.libs.http.config.RestTemplateConfiguration
import no.nav.tilleggsstonader.libs.log.filter.LogFilterConfiguration
import no.nav.tilleggsstonader.libs.unleash.UnleashConfiguration
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringBootConfiguration
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootConfiguration
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
@EnableOAuth2Client(cacheEnabled = true)
@Import(
    RestTemplateConfiguration::class,
    LogFilterConfiguration::class,
    UnleashConfiguration::class,
)
@EnableScheduling
class ApplicationConfig {

    private val logger = LoggerFactory.getLogger(javaClass)
    init {
        logger.info("Starter versjon=${Applikasjonsversjon.versjon}")
    }
}
