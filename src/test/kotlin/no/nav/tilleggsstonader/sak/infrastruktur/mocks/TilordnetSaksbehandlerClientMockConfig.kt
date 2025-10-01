package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.Saksbehandler
import no.nav.tilleggsstonader.sak.opplysninger.tilordnetSaksbehandler.TilordnetSaksbehandlerClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.test.util.AopTestUtils
import java.util.UUID

@Configuration
@Profile("mock-oppgave")
class TilordnetSaksbehandlerClientMockConfig {
    @Bean
    @Primary
    fun tilordnetSaksbehandlerClient() = mockk<TilordnetSaksbehandlerClient>().apply { resetTilDefault(this) }

    companion object {
        fun resetTilDefault(client: TilordnetSaksbehandlerClient) {
            // Da TilordnetSaksbehandlerClient har en @Cachable-metode, driver Spring og lager proxier av klassen.
            // Må derfor hente ut den underliggende mocken:
            val faktiskClient = runCatching { AopTestUtils.getTargetObject<TilordnetSaksbehandlerClient>(client) }.getOrElse { client }

            clearMocks(faktiskClient)
            every { faktiskClient.hentSaksbehandlerInfo(any()) } returns
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
