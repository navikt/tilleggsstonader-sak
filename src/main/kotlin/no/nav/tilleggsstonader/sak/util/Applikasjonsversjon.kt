package no.nav.tilleggsstonader.sak.util

import java.util.Properties

object Applikasjonsversjon {
    val versjon: String

    init {
        val file = this::class.java.classLoader.getResourceAsStream("version.properties")
        val properties = Properties()
        properties.load(file)
        versjon = properties.getProperty("project.version")
    }
}
