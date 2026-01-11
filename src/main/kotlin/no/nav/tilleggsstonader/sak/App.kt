package no.nav.tilleggsstonader.sak

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration

@SpringBootApplication(
    exclude = [ErrorMvcAutoConfiguration::class],
)
class App

fun main(args: Array<String>) {
    runApplication<App>(*args)
}
