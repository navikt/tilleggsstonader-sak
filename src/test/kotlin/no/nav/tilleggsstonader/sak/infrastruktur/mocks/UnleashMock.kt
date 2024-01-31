package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.mockUnleashService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Profile("mock-featuretoggle")
@Configuration
class UnleashMock {

    private val logger = LoggerFactory.getLogger(UnleashMock::class.java)

    @Bean
    @Primary
    fun unleashService(): UnleashService {
        logger.info("Oppretter mock-UnleashService")
        return mockUnleashService()
    }
}
