package no.nav.tilleggsstonader.sak.infrastruktur.config

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ObjectMapperProviderTest {

    val expectedJson = """
        {
          "string" : "verdi",
          "manglerVerdi" : null,
          "dato" : "2023-01-01",
          "tidspunkt" : "2023-01-01T04:04:00",
          "liste" : [ {
            "string" : "verdi"
          } ],
          "set" : [ {
            "string" : "verdi"
          } ]
        }
    """.trimIndent()

    private val prettyPrinter = objectMapper.writerWithDefaultPrettyPrinter()

    @Test
    fun `skal parsea json riktig`() {
        val root = Root()
        val json = prettyPrinter.writeValueAsString(root)
        assertThat(json).isEqualTo(expectedJson)

        val rootFraJson = objectMapper.readValue<Root>(json)
        assertThat(prettyPrinter.writeValueAsString(rootFraJson)).isEqualTo(expectedJson)
    }

    @Test
    fun `skal kunne parsea data når et verdi mangler`() {
        assertThat(objectMapper.readValue<ElementMedOptionalFelt>("""{"verdi": "verdi"}"""))
            .isEqualTo(ElementMedOptionalFelt("verdi"))
    }

    @Test
    fun `skal feile hvis et felt mangler verdi`() {
        assertThatThrownBy {
            objectMapper.readValue<ElementMedOptionalFelt>("""{}""")
        }.isInstanceOf(MissingKotlinParameterException::class.java)
    }

    @Test
    fun `skal sette defaultverdi når det finnes`() {
        assertThat(objectMapper.readValue<Element>("""{}"""))
            .isEqualTo(Element("verdi"))
    }
}

private data class Root(
    val string: String = "verdi",
    val manglerVerdi: String? = null,
    val dato: LocalDate = LocalDate.of(2023, 1, 1),
    val tidspunkt: LocalDateTime = LocalDateTime.of(2023, 1, 1, 4, 4, 0),
    val liste: List<Element> = listOf(Element()),
    val set: Set<Element> = setOf(Element()),
)

private data class Element(
    val string: String = "verdi",
)

private data class ElementMedOptionalFelt(
    val verdi: String,
    val optional: String? = null,
)
