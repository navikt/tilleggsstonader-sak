package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnMappeResponseDto
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnOppgaveResponseDto
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Configuration
@Profile("mock-oppgave")
class OppgaveClientConfig {

    @Bean
    @Primary
    fun oppgaveClient(): OppgaveClient {
        val oppgaveClient = mockk<OppgaveClient>()
        val oppgaver = defaultOppgaver()
        every { oppgaveClient.hentOppgaver(any()) } answers {
            FinnOppgaveResponseDto(antallTreffTotalt = oppgaver.size.toLong(), oppgaver = oppgaver.values.toList())
        }
        every { oppgaveClient.finnMapper(any(), any()) } returns FinnMappeResponseDto(0, emptyList())
        mockFordeling(oppgaveClient, oppgaver)
        return oppgaveClient
    }

    private fun mockFordeling(
        oppgaveClient: OppgaveClient,
        oppgaver: MutableMap<Long, Oppgave>,
    ) {
        every { oppgaveClient.fordelOppgave(any(), any(), any()) } answers {
            val oppgaveId = firstArg<Long>()
            val oppgave = oppgaver.getValue(oppgaveId)
            val versjon = oppgave.versjon!!
            feilHvis(versjon != thirdArg(), HttpStatus.CONFLICT) {
                "Oppgaven har endret seg siden du sist hentet oppgaver. " +
                    "For å kunne gjøre endringer må du hente oppgaver på nytt."
            }
            val oppdatertOppgave = oppgave.copy(versjon = versjon + 1, tilordnetRessurs = secondArg())
            oppgaver[oppgaveId] = oppdatertOppgave
            oppdatertOppgave
        }
    }

    private fun defaultOppgaver(): MutableMap<Long, Oppgave> {
        val oppgaver = (0..100L).map {
            Oppgave(
                id = it,
                opprettetTidspunkt = LocalDate.of(2023, 1, 1).plusDays(it).atStartOfDay()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                versjon = 0,
            )
        }
        return oppgaver.associateBy { it.id!! }.toMutableMap()
    }
}
