package no.nav.tilleggsstonader.sak

import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.tilleggsstonader.sak.util.DbContainerInitializer
import org.springframework.boot.builder.SpringApplicationBuilder

/**
 * Starter en lokal instans mot en in-memory database
 */
@EnableMockOAuth2Server
class SakAppLocal : App()

fun main(args: Array<String>) {
    appLocal()
        .initializers(DbContainerInitializer())
        .run(*args)
}

fun appLocal(): SpringApplicationBuilder =
    SpringApplicationBuilder(SakAppLocal::class.java)
        .profiles(
            "local",
            "mock-pdl",
            "mock-egen-ansatt",
            "mock-iverksett",
            "mock-oppgave",
            "mock-featuretoggle",
            "mock-journalpost",
            "mock-htmlify",
            "mock-arena",
            "mock-aktivitet",
            "mock-kodeverk",
            "mock-arbeidsfordeling",
            "mock-kafka",
            "mock-familie-dokument",
            "mock-ytelse-client",
        )
