package no.nav.tilleggsstonader.sak.behandlingsflyt

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StegTypeTest {
    @Test
    fun `rekkefølgen på stegene skal være økende med 1 mellom hvert steg`() {
        StegType.entries.windowed(2).forEach {
            assertThat(it[0].rekkefølge + 1)
                .`as` {
                    "${it[0]} med rekkefølge ${it[0].rekkefølge} " +
                        "testes mot ${it[1]} med rekkefølge=${it[1].rekkefølge}"
                }.isEqualTo(it[1].rekkefølge)
        }
    }
}
