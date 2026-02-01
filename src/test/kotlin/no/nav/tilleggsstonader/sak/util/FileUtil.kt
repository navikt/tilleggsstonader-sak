package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider
import org.assertj.core.api.Assertions.assertThat
import tools.jackson.databind.JsonNode
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isDirectory

object FileUtil {
    fun readFile(filnavn: String): String =
        FileUtil::class.java.classLoader
            .getResource(filnavn)
            ?.readText()
            ?: error("Finner ikke fil: $filnavn")

    fun listFiles(path: String): List<Path> {
        val uri =
            FileUtil::class.java.classLoader
                .getResource(path)!!
                .toURI()
        return Files.list(Paths.get(uri)).map { it.fileName }.toList()
    }

    fun listDir(path: String): List<Path> {
        val uri =
            FileUtil::class.java.classLoader
                .getResource(path)!!
                .toURI()
        return Files
            .list(Paths.get(uri))
            .filter { it.isDirectory() }
            .map { it.fileName }
            .toList()
    }

    /**
     * Denne kan endres hvis man ønsker å skrive over filer som brukes i tester, eks:
     * 1. Tester feiler pga endringer
     * 2. Setter denne til true
     * 3. Kjører tester på nytt, 2 ganger, 1 gang for å skrive filen, en andre gång for å verifisere
     * 4. set denne til false på nytt, hvis ikke feiler [FileUtilTest]
     *
     * Kan også settes via environment variable: SKRIV_TIL_FIL=true
     */
    val SKRIV_TIL_FIL = System.getenv("SKRIV_TIL_FIL")?.toBoolean() ?: false

    fun assertFileJsonIsEqual(
        filnavn: String,
        json: Any,
    ) {
        val jsonNode = JsonMapperProvider.jsonMapper.valueToTree<JsonNode>(json)
        val filJsonNode = JsonMapperProvider.jsonMapper.readTree(readFile(filnavn))

        skrivTilFil(filnavn, jsonNode.toString())
        assertThat(jsonNode).isEqualTo(filJsonNode)
    }

    fun assertFileIsEqual(
        filnavn: String,
        data: Any,
    ) {
        val json = JsonMapperProvider.jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data)
        assertFileIsEqual(filnavn, json)
    }

    fun assertFileIsEqual(
        filnavn: String,
        data: String,
    ) {
        skrivTilFil(filnavn, data)
        assertThat(data).isEqualTo(readFile(filnavn))
    }

    fun skrivTilFil(
        filnavn: String,
        data: String,
    ) {
        skrivTilFil(filnavn, data.toByteArray())
    }

    fun skrivTilFil(
        filnavn: String,
        data: ByteArray,
    ) {
        if (!SKRIV_TIL_FIL) {
            return
        }
        val file = File("src/test/resources/$filnavn")
        if (!file.exists()) {
            file.createNewFile()
        }
        file.writeBytes(data)
    }
}
