package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.spyk
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.SatsBoutgifterProvider
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.SatsLæremidlerProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class SatsBoutgifterMock {
    @Bean
    @Primary
    fun satsBoutgifterProviderMock(): SatsBoutgifterProvider = spyk<SatsBoutgifterProvider>()
}
