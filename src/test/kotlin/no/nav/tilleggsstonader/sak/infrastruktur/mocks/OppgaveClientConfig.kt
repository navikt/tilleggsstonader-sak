package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnMappeResponseDto
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnOppgaveRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnOppgaveResponseDto
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveIdentV2
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.StatusEnum
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Configuration
@Profile("mock-oppgave")
class OppgaveClientConfig {

    var maxOppgaveId = 0L
    val oppgavelager = mutableMapOf<Long, Oppgave>()

    @Bean
    @Primary
    fun oppgaveClient(): OppgaveClient {
        val oppgaveClient = mockk<OppgaveClient>()

        every { oppgaveClient.hentOppgaver(any()) } answers {
            val request = firstArg<FinnOppgaveRequest>()
            val oppgaver = oppgavelager.values
                .filter { it.status == StatusEnum.OPPRETTET }
                .filter { oppgave -> request.behandlingstema?.let { oppgave.behandlingstema == it.value } ?: true }
                .filter { oppgave -> request.oppgavetype?.let { oppgave.oppgavetype == it.value } ?: true }
                .filter { oppgave -> request.behandlingstype?.let { oppgave.behandlingstype == it.value } ?: true }
                .toList()
            FinnOppgaveResponseDto(antallTreffTotalt = oppgavelager.size.toLong(), oppgaver = oppgaver)
        }

        every { oppgaveClient.finnMapper(any(), any()) } returns FinnMappeResponseDto(0, emptyList())

        every { oppgaveClient.opprettOppgave(any()) } answers {
            val oppgave = opprettOppgave(firstArg<OpprettOppgaveRequest>())
            oppgave.id
        }

        every { oppgaveClient.ferdigstillOppgave(any()) } answers {
            val oppgave = oppgavelager.getValue(firstArg())
            if (oppgave.status == StatusEnum.FERDIGSTILT) {
                error("Allerede ferdigstilt")
            }
            val oppdatertOppgave = oppgave.copy(
                versjon = oppgave.versjon!! + 1,
                status = StatusEnum.FERDIGSTILT,
                ferdigstiltTidspunkt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
            )
            oppgavelager[oppgave.id] = oppdatertOppgave
        }

        every { oppgaveClient.oppdaterOppgave(any()) } answers {
            val oppdaterOppgave = firstArg<Oppgave>()
            oppgavelager[oppdaterOppgave.id] = oppdaterOppgave // Forenklet, dette er ikke det som skje ri integrasjoner
            oppdaterOppgave.id
        }

        mockFordeling(oppgaveClient, oppgavelager)
        return oppgaveClient
    }

    private fun opprettOppgave(
        oppgaveDto: OpprettOppgaveRequest,
    ): Oppgave {
        val oppgave = Oppgave(
            id = ++maxOppgaveId,
            versjon = 1,
            status = StatusEnum.OPPRETTET,
            identer = oppgaveDto.ident!!.let { listOf(OppgaveIdentV2(it.ident!!, it.gruppe!!)) },
            tildeltEnhetsnr = oppgaveDto.enhetsnummer,
            saksreferanse = null,
            journalpostId = oppgaveDto.journalpostId,
            tema = oppgaveDto.tema,
            oppgavetype = oppgaveDto.oppgavetype.value,
            behandlingstema = oppgaveDto.behandlingstema,
            tilordnetRessurs = oppgaveDto.tilordnetRessurs,
            fristFerdigstillelse = oppgaveDto.fristFerdigstillelse.format(DateTimeFormatter.ISO_DATE),
            aktivDato = oppgaveDto.aktivFra.format(DateTimeFormatter.ISO_DATE),
            beskrivelse = oppgaveDto.beskrivelse,
            prioritet = oppgaveDto.prioritet,
            behandlingstype = oppgaveDto.behandlingstype,
            behandlesAvApplikasjon = oppgaveDto.behandlesAvApplikasjon,
            mappeId = oppgaveDto.mappeId,
            opprettetTidspunkt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
        )
        oppgavelager[oppgave.id] = oppgave
        return oppgave
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
}
