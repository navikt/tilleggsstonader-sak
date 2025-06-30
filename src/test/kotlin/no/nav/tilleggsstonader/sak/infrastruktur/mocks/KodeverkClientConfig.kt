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
        every { client.hentPostnummer() } returns
            KodeverkDto(
                listOf("0010" to "Oslo").tilBetydninger(),
            )
        every { client.hentLandkoder() } returns
            KodeverkDto(
                listOf(
                    "SWE" to "Sverige",
                    "FIN" to "Finland",
                    "NOR" to "Norge",
                ).tilBetydninger(),
            )

        every { client.hentLandkoderIso2() } returns
            KodeverkDto(
                listOf(
                    "SE" to "Sverige",
                    "FI" to "Finland",
                    "NO" to "Norge",
                ).tilBetydninger(),
            )
        return client
    }

    private fun List<Pair<String, String>>.tilBetydninger() =
        this.associate { it.first to listOf(betydning(BeskrivelseDto(it.second, it.second))) }

    private fun betydning(it: BeskrivelseDto) =
        BetydningDto(
            gyldigFra = LocalDate.now().minusYears(1),
            gyldigTil = LocalDate.now().plusYears(1),
            beskrivelser = mapOf("nb" to it),
        )
}
