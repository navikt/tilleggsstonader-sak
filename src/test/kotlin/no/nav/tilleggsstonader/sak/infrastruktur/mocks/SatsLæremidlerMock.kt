package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.spyk
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.SatsLæremidlerService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class SatsLæremidlerMock {
    @Bean
    @Primary
    fun satsLæremidlerServiceMock(): SatsLæremidlerService = spyk<SatsLæremidlerService>()
}
