package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.kodeverk.BeskrivelseDto
import no.nav.tilleggsstonader.kontrakter.kodeverk.BetydningDto
import no.nav.tilleggsstonader.kontrakter.kodeverk.KodeverkDto
import no.nav.tilleggsstonader.sak.opplysninger.kodeverk.KodeverkClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.time.LocalDate

@Configuration
@Profile("mock-kodeverk")
class KodeverkClientConfig {

    @Bean
    fun kodeverkClient(): KodeverkClient {
        val client = mockk<KodeverkClient>()
        every { client.hentPostnummer() } returns KodeverkDto(
            mapOf(
                "Postnummer" to listOf(
                    BeskrivelseDto("0010", "Oslo"),
                ).map { betydning(it) },
            ),
        )
        every { client.hentLandkoder() } returns KodeverkDto(
            mapOf(
                "Landkoder" to listOf(
                    BeskrivelseDto("SWE", "Sverige"),
                    BeskrivelseDto("FIN", "Finland"),
                ).map { betydning(it) },
            ),
        )

        return client
    }

    private fun betydning(it: BeskrivelseDto) =
        BetydningDto(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1), mapOf("nb" to it))
}
