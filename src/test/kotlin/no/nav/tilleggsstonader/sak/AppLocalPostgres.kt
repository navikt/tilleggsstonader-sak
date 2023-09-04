package no.nav.tilleggsstonader.sak

import java.util.Properties

/**
 * Starter en lokal instans mot en kjørendes postgres-instans, som startet med eks `docker-compose up`
 */
fun main(args: Array<String>) {
    appLocal()
        .properties(Properties().medDatabase())
        .run(*args)
}

private fun Properties.medDatabase(): Properties {
    setProperty("DATASOURCE_URL", "jdbc:postgresql://localhost:5432/tilleggsstonader-sak")
    setProperty("DATASOURCE_USERNAME", "postgres")
    setProperty("DATASOURCE_PASSWORD", "test")
    setProperty("DATASOURCE_DRIVER", "org.postgresql.Driver")

    return this
}
