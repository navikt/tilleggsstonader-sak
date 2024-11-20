package no.nav.tilleggsstonader.sak.vedtak.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TypeVedtaksdataTest {

    @Test
    fun `typerVedtaksdata må inneholde unike navn`() {
        val alleEnums = listOf(
            TypeVedtakTilsynBarn.entries,
        ).flatten()
        assertThat(typerVedtaksdata.keys)
            .hasSize(alleEnums.size)
    }
}
