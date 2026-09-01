package no.nav.tilleggsstonader.sak

import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.EksternApplikasjon
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration

@SpringBootApplication(
    exclude = [ErrorMvcAutoConfiguration::class],
)
@EnableConfigurationProperties(EksternApplikasjon::class)
class App

fun main(args: Array<String>) {
    // Avro 1.12.2 krever eksplisitt tillit til pakker som brukes i genererte schema-klasser,
    // se https://issues.apache.org/jira/browse/AVRO-3971 (CVE-2024-47561)
    System.setProperty("org.apache.avro.SERIALIZABLE_PACKAGES", "no.nav.person.pdl.leesah,no.nav.joarkjournalfoeringhendelser")
    runApplication<App>(*args)
}
