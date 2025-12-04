package no.nav.tilleggsstonader.sak.infrastruktur.config

import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.jsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import tools.jackson.databind.exc.MismatchedInputException
import tools.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.time.LocalDateTime

class ObjectMapperProviderTest {
    val expectedJson =
        """
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

    private val prettyPrinter = jsonMapper.writerWithDefaultPrettyPrinter()

    @Test
    fun `skal parsea json riktig`() {
        val root = Root()
        val json = prettyPrinter.writeValueAsString(root)
        assertThat(json).isEqualTo(expectedJson)

        val rootFraJson = jsonMapper.readValue<Root>(json)
        assertThat(prettyPrinter.writeValueAsString(rootFraJson)).isEqualTo(expectedJson)
    }

    @Test
    fun `skal kunne parsea data når et verdi mangler`() {
        assertThat(jsonMapper.readValue<ElementMedOptionalFelt>("""{"verdi": "verdi"}"""))
            .isEqualTo(ElementMedOptionalFelt("verdi"))
    }

    @Test
    fun `skal feile hvis et felt mangler verdi`() {
        assertThatThrownBy {
            jsonMapper.readValue<ElementMedOptionalFelt>("""{}""")
        }.isInstanceOf(MismatchedInputException::class.java)
    }

    @Test
    fun `skal sette defaultverdi når det finnes`() {
        assertThat(jsonMapper.readValue<Element>("""{}"""))
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
