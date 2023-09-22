package no.nav.tilleggsstonader.sak.util

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object FileUtil {
    fun readFile(filnavn: String): String =
        FileUtil::class.java.classLoader.getResource(filnavn)?.readText()
            ?: error("Finner ikke fil: $filnavn")

    fun listFiles(path: String): List<Path> {
        val uri = FileUtil::class.java.classLoader.getResource(path)!!.toURI()
        return Files.list(Paths.get(uri)).map { it.fileName }.toList()
    }
}
