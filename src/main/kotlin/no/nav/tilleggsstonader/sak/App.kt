package no.nav.tilleggsstonader.sak

import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.EksternApplikasjon
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication(
    exclude = [ErrorMvcAutoConfiguration::class],
)
@EnableConfigurationProperties(EksternApplikasjon::class)
class App

fun main(args: Array<String>) {
    runApplication<App>(*args)
}
