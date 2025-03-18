package no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.util.EnumUtil.enumName
import no.nav.tilleggsstonader.sak.util.FileUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import kotlin.io.path.name
import kotlin.reflect.full.findAnnotation

class FaktaGrunnlagDataTest {
    val jsonSubTypes = FaktaGrunnlagDataJson::class.findAnnotation<JsonSubTypes>()!!

    val alleEnums = TypeFaktaGrunnlag.entries

    @Test
    fun `sjekk at alle VedtakJson er mappet`() {
        assertThat(jsonSubTypes.value.map { it.name })
            .containsExactlyInAnyOrderElementsOf(alleEnums.map { it.enumName() })
    }

    @Test
    fun `sjekk at det finnes json-tester for alle typer`() {
        assertThat(jsonFiler.map { it })
            .containsExactlyInAnyOrderElementsOf(jsonSubTypes.value.map { "${it.name}.json" })
    }

    @ParameterizedTest
    @MethodSource("jsonFilProvider")
    fun `sjekk at deserialisering av json fungerer som forventet`(fil: String) {
        val json = FileUtil.readFile("faktaGrunnlag/$fil")

        val parsetJson = objectMapper.readValue<FaktaGrunnlagData>(json)

        val jsonFraObj = objectMapper.convertValue<Map<String, Any>>(parsetJson).toSortedMap()
        val jsonFraFil = objectMapper.readValue<Map<String, Any>>(json).toSortedMap()

        assertThat(parsetJson).isInstanceOf(parsetJson.type.kClass.java)
        assertThat(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonFraFil))
            .isEqualTo(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonFraObj))
    }

    @Disabled
    @Test
    fun `opprett dummy-filer`() {
        alleEnums.forEach { type ->
            val enumName = type.enumName()
            val dir = "src/test/resources/faktaGrunnlag"
            val file = File("$dir/$enumName.json")
            if (!file.exists()) {
                println("${file.name}: Oppretter")
                file.createNewFile()
                file.writeText("{\n  \"type\": \"$enumName\",\n  \"\": \n}")
            } else {
                println("${file.name}: Eksisterer, oppretter ikke")
            }
        }
    }

    companion object {
        private val jsonFiler = FileUtil.listFiles("faktaGrunnlag").map { it.fileName.name }

        @JvmStatic
        private fun jsonFilProvider(): List<String> = jsonFiler
    }
}
