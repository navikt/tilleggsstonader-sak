package no.nav.tilleggsstonader.sak.felles.domain

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FagsakIdTest {

    val id = "96673a99-1d90-4f22-abdf-faee57062432"

    @Test
    fun `toString skal returnere verdiet på UUID`() {
        assertThat(FagsakId.fromString(id).toString()).isEqualTo(id)
    }

    @Test
    fun `skal håndteres riktig i fra og til json`() {
        val foo = Foo(FagsakId.fromString(id))
        val json = objectMapper.writeValueAsString(foo)

        assertThat(json).isEqualTo("""{"id":"96673a99-1d90-4f22-abdf-faee57062432"}""")
        assertThat(objectMapper.readValue<Foo>(json)).isEqualTo(foo)
    }

    private data class Foo(
        val id: FagsakId,
    )
}
