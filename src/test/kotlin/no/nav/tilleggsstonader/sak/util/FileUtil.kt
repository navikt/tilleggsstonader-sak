package no.nav.tilleggsstonader.sak.util

object FileUtil {
    fun readFile(filnavn: String): String =
        FileUtil::class.java.classLoader.getResource(filnavn)?.readText()
            ?: error("Finner ikke fil: $filnavn")
}
