package no.nav.tilleggsstonader.sak.infrastruktur

import no.nav.tilleggsstonader.sak.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DbTest : IntegrationTest() {
    @Test
    fun `ping mot database`() {
        val result = jdbcTemplate.query("select 'ja' result") { rs, _ -> rs.getString("result") }
        assertThat(result.single()).isEqualTo("ja")
    }
}
