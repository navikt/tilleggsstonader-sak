package no.nav.tilleggsstonader.sak.opplysninger.kodeverk

import no.nav.tilleggsstonader.sak.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class KodeverkServiceIntegrationTest : IntegrationTest() {
    @Autowired
    lateinit var kodeverkService: KodeverkService

    @Test
    fun `skal mapppe landkode til land`() {
        assertThat(kodeverkService.hentLandkode("SWE")).isEqualTo("Sverige")
        assertThat(kodeverkService.hentLandkode("FIN")).isEqualTo("Finland")
    }

    @Test
    fun `skal mappe ikke-eksisterende landkode til koden`() {
        assertThat(kodeverkService.hentLandkode("EKSISTERER_IKKE")).isEqualTo("EKSISTERER_IKKE")
    }

    @Test
    fun `skal mappe landkode null til tom streng`() {
        assertThat(kodeverkService.hentLandkode(null)).isEqualTo("")
    }
}
