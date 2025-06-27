package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.Saksbehandler
import no.nav.tilleggsstonader.sak.opplysninger.ansvarligSaksbehandler.AnsvarligSaksbehandlerClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.util.UUID

@Configuration
@Profile("mock-oppgave")
class SaksbehandlerClientConfig {
    @Bean
    @Primary
    fun saksbehandlerClient(): AnsvarligSaksbehandlerClient {
        val saksbehandlerClient = mockk<AnsvarligSaksbehandlerClient>()
        resetMock(saksbehandlerClient)
        return saksbehandlerClient
    }

    companion object {
        fun resetMock(saksbehandlerClient: AnsvarligSaksbehandlerClient) {
            clearMocks(saksbehandlerClient)

            every { saksbehandlerClient.hentSaksbehandlerInfo(any()) } returns
                Saksbehandler(
                    navIdent = "Z993543",
                    azureId = UUID.randomUUID(),
                    fornavn = "Test",
                    etternavn = "Testesen",
                    enhet = "TSO",
                )
        }
    }
}
