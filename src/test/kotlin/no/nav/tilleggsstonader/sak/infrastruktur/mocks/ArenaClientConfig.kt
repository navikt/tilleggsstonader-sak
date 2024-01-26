package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.arena.SakStatus
import no.nav.tilleggsstonader.kontrakter.arena.VedtakStatus
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-arena")
class ArenaClientConfig {

    @Bean
    @Primary
    fun arenaClient(): ArenaClient {
        val client = mockk<ArenaClient>()
        every { client.hentStatus(any()) } returns ArenaStatusDto(
            SakStatus(harSaker = false, harÅpenSak = false),
            VedtakStatus(harVedtak = false, harAktivtVedtak = false),
        )
        return client
    }
}
