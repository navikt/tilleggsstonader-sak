package no.nav.tilleggsstonader.sak

import org.springframework.boot.builder.SpringApplicationBuilder

fun main(args: Array<String>) {
    SpringApplicationBuilder(AppLocal::class.java)
        .profiles(
            "local",
        )
        .run(*args)
}
