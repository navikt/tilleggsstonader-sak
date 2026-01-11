package no.nav.tilleggsstonader.sak.infrastruktur.jackson

import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig {
    @Bean
    fun jsonMapper() = JsonMapperProvider.jsonMapper
}
