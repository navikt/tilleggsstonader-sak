package no.nav.tilleggsstonader.sak.utbetaling.fagomrade

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FagsakUtbetalingsvalgServiceTest {
    private val fagsakService = mockk<FagsakService>()
    private val unleashService = mockk<UnleashService>()
    private val fagsakUtbetalingsvalgService = FagsakUtbetalingsvalgService(fagsakService, unleashService)
    private val fagsakId = FagsakId.random()

    @Test
    fun `daglig reise skal lagres med nytt fagområde`() {
        every { fagsakService.hentFagsak(fagsakId) } returns fagsak(utbetalPåNyttFagområde = null)
        every { fagsakService.settUtbetalPåNyttFagområde(fagsakId, true) } returns true

        fagsakUtbetalingsvalgService.hentEllerSettUtbetalPåNyttFagområde(
            fagsakId = fagsakId,
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        )

        verify { fagsakService.settUtbetalPåNyttFagområde(fagsakId, true) }
        verify(exactly = 0) { unleashService.isEnabled(any<Toggle>()) }
    }

    @Test
    fun `ikke-daglig reise med toggle av skal lagres med gammelt fagområde`() {
        every { fagsakService.hentFagsak(fagsakId) } returns fagsak(utbetalPåNyttFagområde = null)
        every { unleashService.isEnabled(Toggle.BRUK_NYTT_FAGOMRADE_FOR_UTBETALING) } returns false
        every { fagsakService.settUtbetalPåNyttFagområde(fagsakId, false) } returns false

        fagsakUtbetalingsvalgService.hentEllerSettUtbetalPåNyttFagområde(
            fagsakId = fagsakId,
            stønadstype = Stønadstype.BOUTGIFTER,
        )

        verify { fagsakService.settUtbetalPåNyttFagområde(fagsakId, false) }
    }

    @Test
    fun `ikke-daglig reise med toggle på skal lagres med nytt fagområde`() {
        every { fagsakService.hentFagsak(fagsakId) } returns fagsak(utbetalPåNyttFagområde = null)
        every { unleashService.isEnabled(Toggle.BRUK_NYTT_FAGOMRADE_FOR_UTBETALING) } returns true
        every { fagsakService.settUtbetalPåNyttFagområde(fagsakId, true) } returns true

        fagsakUtbetalingsvalgService.hentEllerSettUtbetalPåNyttFagområde(
            fagsakId = fagsakId,
            stønadstype = Stønadstype.BOUTGIFTER,
        )

        verify { fagsakService.settUtbetalPåNyttFagområde(fagsakId, true) }
    }

    @Test
    fun `gjenbruker eksisterende verdi på fagsak uten å oppdatere eller evaluere toggle`() {
        every { fagsakService.hentFagsak(fagsakId) } returns fagsak(utbetalPåNyttFagområde = true)

        val utbetalPåNyttFagområde =
            fagsakUtbetalingsvalgService.hentEllerSettUtbetalPåNyttFagområde(
                fagsakId = fagsakId,
                stønadstype = Stønadstype.BOUTGIFTER,
            )

        assertThat(utbetalPåNyttFagområde).isTrue
        verify(exactly = 0) { fagsakService.settUtbetalPåNyttFagområde(any(), any()) }
        verify(exactly = 0) { unleashService.isEnabled(any<Toggle>()) }
    }

    private fun fagsak(utbetalPåNyttFagområde: Boolean?): Fagsak =
        mockk {
            every { this@mockk.utbetalPåNyttFagområde } returns utbetalPåNyttFagområde
        }
}
