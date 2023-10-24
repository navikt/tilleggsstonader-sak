package no.nav.tilleggsstonader.sak.arbeidsfordeling

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

internal class ArbeidsfordelingServiceTest {

    private lateinit var arbeidsfordelingClient: ArbeidsfordelingClient
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @BeforeEach
    internal fun setUp() {
        arbeidsfordelingClient = mockk()
        every { arbeidsfordelingClient.hentNavEnhetForPersonMedRelasjoner(any()) } returns listOf()
        every { arbeidsfordelingClient.hentBehandlendeEnhetForOppfølging(any()) } returns Arbeidsfordelingsenhet("", "")
        val cacheManager = ConcurrentMapCacheManager()
        arbeidsfordelingService = ArbeidsfordelingService(arbeidsfordelingClient, cacheManager)
    }

    @Test
    internal fun `skal hente arbeidsfordeling`() {
        arbeidsfordelingService.hentNavEnhet(IDENT_FORELDER)
        verify(exactly = 1) { arbeidsfordelingClient.hentNavEnhetForPersonMedRelasjoner(IDENT_FORELDER) }
    }

    @Test
    internal fun `hentNavEnhet - skal cache når man kaller på den indirekte`() {
        arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(IDENT_FORELDER)
        arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(IDENT_FORELDER)
        verify(exactly = 1) { arbeidsfordelingClient.hentNavEnhetForPersonMedRelasjoner(IDENT_FORELDER) }

        arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(IDENT_BARN)
        verify(exactly = 1) { arbeidsfordelingClient.hentNavEnhetForPersonMedRelasjoner(IDENT_BARN) }
    }

    @Test
    internal fun `hentNavEnhetId - skal bruke hentNavEnhetForOppfølging dersom oppgavetype er VurderHenvendelse`() {
        arbeidsfordelingService.hentNavEnhetId(IDENT_FORELDER, Oppgavetype.VurderHenvendelse)
        arbeidsfordelingService.hentNavEnhetId(IDENT_FORELDER, Oppgavetype.VurderHenvendelse)
        verify(exactly = 1) { arbeidsfordelingClient.hentBehandlendeEnhetForOppfølging(IDENT_FORELDER) }

        arbeidsfordelingService.hentNavEnhetId(IDENT_BARN, Oppgavetype.VurderHenvendelse)
        verify(exactly = 1) { arbeidsfordelingClient.hentBehandlendeEnhetForOppfølging(IDENT_BARN) }
    }

    @Test
    internal fun `hentNavEnhetId - skal bruke hentNavEnhet dersom oppgavetype ikke er VurderHenvendelse`() {
        arbeidsfordelingService.hentNavEnhetId(IDENT_FORELDER, Oppgavetype.BehandleSak)
        arbeidsfordelingService.hentNavEnhetId(IDENT_FORELDER, Oppgavetype.BehandleSak)
        verify(exactly = 1) { arbeidsfordelingClient.hentNavEnhetForPersonMedRelasjoner(IDENT_FORELDER) }

        arbeidsfordelingService.hentNavEnhetId(IDENT_BARN, Oppgavetype.BehandleSak)
        verify(exactly = 1) { arbeidsfordelingClient.hentNavEnhetForPersonMedRelasjoner(IDENT_BARN) }
    }

    companion object {

        private const val IDENT_FORELDER = "1"
        private const val IDENT_BARN = "2"
    }
}
