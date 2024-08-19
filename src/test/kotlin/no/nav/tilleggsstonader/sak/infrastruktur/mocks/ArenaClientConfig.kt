package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusHarSakerDto
import no.nav.tilleggsstonader.kontrakter.arena.SakStatus
import no.nav.tilleggsstonader.kontrakter.arena.VedtakStatus
import no.nav.tilleggsstonader.kontrakter.arena.oppgave.ArenaOppgaveDto
import no.nav.tilleggsstonader.kontrakter.arena.sak.Målgruppe
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate
import java.time.LocalDateTime

@Configuration
@Profile("mock-arena")
class ArenaClientConfig {

    @Bean
    @Primary
    fun arenaClient(): ArenaClient {
        val client = mockk<ArenaClient>()
        resetMock(client)
        return client
    }

    companion object {
        fun resetMock(client: ArenaClient) {
            clearMocks(client)
            every { client.hentStatus(any()) } returns ArenaStatusDto(
                SakStatus(harAktivSakUtenVedtak = false),
                VedtakStatus(
                    harVedtak = false,
                    harAktivtVedtak = false,
                    harVedtakUtenUtfall = false,
                    vedtakTom = LocalDate.now().minusDays(10),
                ),
            )
            every { client.harSaker(any()) } returns ArenaStatusHarSakerDto(true)
            every { client.hentOppgaver(any()) } returns listOf(
                ArenaOppgaveDto(
                    id = 1,
                    tittel = "Kontroller/registrer saksopplysninger - automatisk journalført",
                    kommentar = "En kommentar\\n\\n med radbryte",
                    benk = "Inn",
                    tildelt = null,
                    opprettetTidspunkt = LocalDateTime.now(),
                ),
                ArenaOppgaveDto(
                    id = 2,
                    tittel = "Vurder dokument",
                    kommentar = "En kommentar",
                    benk = null,
                    tildelt = "ABC1234",
                    opprettetTidspunkt = LocalDateTime.now(),
                ),
            )
        }
    }
}
