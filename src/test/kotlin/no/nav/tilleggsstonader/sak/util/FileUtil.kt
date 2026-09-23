package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider
import org.assertj.core.api.Assertions.assertThat
import tools.jackson.module.kotlin.convertValue
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

    /**
     * Sammenligner [json] mot json i [filnavn].
     *
     * Sammenligningen gjøres ved å normalisere begge sider til en sortert Map/List-struktur og deretter
     * pretty-printe dem til strenger, i stedet for å sammenligne [tools.jackson.databind.JsonNode] direkte. Dette er bevisst:
     * - En pretty-printet json-string gir en langt mer lesbar diff enn å sammenligne JsonNode-trær.
     * - Talltyper kan divergere mellom en json-fil (som f.eks. deserialiseres til Int) og et kotlin-objekt
     *   (som f.eks. har et felt av typen BigInteger). JsonNode-likhet skiller på nodetype (IntNode != BigIntegerNode)
     *   selv om verdien er den samme, mens en tekstlig sammenligning av json ikke gjør det.
     *
     * Fungerer uansett om roten i json er et objekt eller en array.
     */
    fun assertFileJsonIsEqual(
        filnavn: String,
        json: Any,
    ) {
        val forventetJson = JsonMapperProvider.jsonMapper.readTree(readFile(filnavn)).tilSortertPrettyJson()
        val faktiskJson = json.tilSortertPrettyJson()

        skrivTilFil(filnavn, faktiskJson)
        assertThat(faktiskJson).isEqualTo(forventetJson)
    }

    private fun Any.tilSortertPrettyJson(): String {
        val sortert = JsonMapperProvider.jsonMapper.convertValue<Any?>(this).toDeepSorted()
        return JsonMapperProvider.jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sortert)
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
