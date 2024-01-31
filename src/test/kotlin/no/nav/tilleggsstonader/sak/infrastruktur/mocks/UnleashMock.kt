package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.getunleash.Variant
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
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
        val mockk = mockk<UnleashService>()
        every { mockk.isEnabled(any()) } returns true
        every { mockk.getVariant(Toggle.SØKNAD_ROUTING_TILSYN_BARN) } returns Variant("antall", "100", true)
        return mockk
    }
}