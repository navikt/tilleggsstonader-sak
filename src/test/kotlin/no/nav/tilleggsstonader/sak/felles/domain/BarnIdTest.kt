package no.nav.tilleggsstonader.sak.felles.domain

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BarnIdTest {
    val id = "96673a99-1d90-4f22-abdf-faee57062432"

    @Test
    fun `toString skal returnere verdiet på UUID`() {
        assertThat(BarnId.fromString(id).toString()).isEqualTo(id)
    }

    @Test
    fun `skal håndteres riktig i fra og til json`() {
        val foo = Foo(BarnId.fromString(id))
        val json = objectMapper.writeValueAsString(foo)

        assertThat(json).isEqualTo("""{"id":"96673a99-1d90-4f22-abdf-faee57062432"}""")
        assertThat(objectMapper.readValue<Foo>(json)).isEqualTo(foo)
    }

    /**
     * Jackson håndterte ikke tidligere deserialisering av en map av eks Map<BarnId, AnnenData>
     * https://github.com/FasterXML/jackson-databind/issues/4444
     */
    @Test
    fun `skal kunne deserialisere som nøkkel i json`() {
        val foo = mapOf(BarnId.fromString(id) to "id")
        val json = objectMapper.writeValueAsString(foo)

        assertThat(json).isEqualTo("""{"96673a99-1d90-4f22-abdf-faee57062432":"id"}""")
        assertThat(objectMapper.readValue<Map<BarnId, String>>(json)).isEqualTo(foo)
    }

    private data class Foo(
        val id: BarnId,
    )
}
