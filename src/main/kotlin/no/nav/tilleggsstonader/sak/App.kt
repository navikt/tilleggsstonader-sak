package no.nav.tilleggsstonader.sak

import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@SpringBootApplication(
    exclude = [ErrorMvcAutoConfiguration::class],
)
class App

fun main(args: Array<String>) {
    runApplication<App>(*args)
}

@Configuration
class JacksonConfig {
    @Bean
    fun jsonMapper() = ObjectMapperProvider.jsonMapper
}
