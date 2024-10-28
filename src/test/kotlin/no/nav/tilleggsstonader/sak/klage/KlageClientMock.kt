package no.nav.tilleggsstonader.sak.klage

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingStatus
import no.nav.tilleggsstonader.kontrakter.klage.KlagebehandlingDto
import no.nav.tilleggsstonader.kontrakter.klage.Årsak
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Configuration
class KlageClientMock {
    @Profile("mock-klage")
    @Bean
    @Primary
    fun klageClient(): KlageClient {
        val mockk = mockk<KlageClient>()
        justRun { mockk.opprettKlage(any()) }
        every { mockk.hentKlagebehandlinger(any()) } answers {
            firstArg<Set<Long>>().associateWith {
                listOf(
                    KlagebehandlingDto(
                        id = UUID.randomUUID(),
                        fagsakId = UUID.randomUUID(),
                        status = BehandlingStatus.FERDIGSTILT,
                        opprettet = LocalDateTime.now(),
                        mottattDato = LocalDate.now(),
                        resultat = BehandlingResultat.IKKE_MEDHOLD,
                        årsak = Årsak.FEIL_I_LOVANDVENDELSE,
                        vedtaksdato = LocalDateTime.now(),
                    ),
                )
            }
        }
        every { mockk.hentBehandlingerForOppgaveIder(any()) } returns emptyMap()
        return mockk
    }
}
