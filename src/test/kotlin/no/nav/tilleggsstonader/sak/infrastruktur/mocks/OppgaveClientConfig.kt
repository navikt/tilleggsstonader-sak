package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnMappeResponseDto
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnOppgaveResponseDto
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-oppgave")
class OppgaveClientConfig {

    @Bean
    @Primary
    fun oppgaveClient(): OppgaveClient {
        val oppgaveClient = mockk<OppgaveClient>()
        every { oppgaveClient.hentOppgaver(any()) } returns FinnOppgaveResponseDto(0, emptyList())
        every { oppgaveClient.finnMapper(any(), any()) } returns FinnMappeResponseDto(0, emptyList())
        return oppgaveClient
    }
}
