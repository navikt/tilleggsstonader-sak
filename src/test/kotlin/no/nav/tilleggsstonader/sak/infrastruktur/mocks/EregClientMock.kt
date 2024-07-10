package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.opplysninger.ereg.EregClient
import no.nav.tilleggsstonader.sak.opplysninger.ereg.Navn
import no.nav.tilleggsstonader.sak.opplysninger.ereg.OrganisasjonDto
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
class EregClientMock {

    @Profile("mock-ereg")
    @Bean
    @Primary
    fun eregClient(): EregClient {
        val mockk = mockk<EregClient>()
        every { mockk.hentOrganisasjoner(any()) } returns lagResponse()
        return mockk
    }

    private fun lagResponse() = OrganisasjonDto(
        organisasjonsnummer = "990983666",
        navn = Navn(navnelinje1 = "Julenissens Gavefabrikk AS"),
    )
}
