package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.spyk
import no.nav.tilleggsstonader.sak.vedtak.sats.SatsPrivatBilProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class SatsDagligReisePrivatBilMock {
    @Bean
    @Primary
    fun satsDagligReisePrivatBilProviderMock(): SatsPrivatBilProvider = spyk<SatsPrivatBilProvider>()
}
