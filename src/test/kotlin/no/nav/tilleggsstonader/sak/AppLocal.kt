package no.nav.tilleggsstonader.sak

import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.springframework.boot.builder.SpringApplicationBuilder

@EnableMockOAuth2Server
class AppLocal : App()

fun main(args: Array<String>) {
    SpringApplicationBuilder(AppLocal::class.java)
        .profiles(
            "local",
        )
        .run(*args)
}
