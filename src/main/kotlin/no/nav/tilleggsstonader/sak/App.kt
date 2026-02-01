package no.nav.tilleggsstonader.sak

import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.EksternApplikasjon
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties


@SpringBootApplication(
    exclude = [ErrorMvcAutoConfiguration::class],
)
@EnableConfigurationProperties(EksternApplikasjon::class)
class App

fun main(args: Array<String>) {
    runApplication<App>(*args)
}
