package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.kodeverk.BeskrivelseDto
import no.nav.tilleggsstonader.kontrakter.kodeverk.BetydningDto
import no.nav.tilleggsstonader.kontrakter.kodeverk.KodeverkDto
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.opplysninger.kodeverk.KodeverkClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

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
                ).tilBetydninger(),
            )
        return client
    }

    private fun List<Pair<String, String>>.tilBetydninger() =
        this.associate { it.first to listOf(betydning(BeskrivelseDto(it.second, it.second))) }

    private fun betydning(it: BeskrivelseDto) = BetydningDto(osloDateNow().minusYears(1), osloDateNow().plusYears(1), mapOf("nb" to it))
}
