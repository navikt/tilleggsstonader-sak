package no.nav.tilleggsstonader.sak.utbetaling.fagomrade

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FagsakUtbetalingsvalgServiceTest {
    private val fagsakService = mockk<FagsakService>()
    private val fagsakUtbetalingsvalgService = FagsakUtbetalingsvalgService(fagsakService)
    private val fagsakId = FagsakId.random()

    @Test
    fun `fagsaker uten verdi skal lagres med nytt fagområde`() {
        every { fagsakService.hentFagsak(fagsakId) } returns fagsak(utbetalPåNyttFagområde = null)
        every { fagsakService.settUtbetalPåNyttFagområde(fagsakId, true) } returns true

        val utbetalPåNyttFagområde =
            fagsakUtbetalingsvalgService.hentEllerSettUtbetalPåNyttFagområde(
                fagsakId = fagsakId,
            )

        assertThat(utbetalPåNyttFagområde).isTrue
        verify { fagsakService.settUtbetalPåNyttFagområde(fagsakId, true) }
    }

    @Test
    fun `gjenbruker eksisterende verdi på fagsak når den er satt`() {
        every { fagsakService.hentFagsak(fagsakId) } returns fagsak(utbetalPåNyttFagområde = false)

        val utbetalPåNyttFagområde =
            fagsakUtbetalingsvalgService.hentEllerSettUtbetalPåNyttFagområde(
                fagsakId = fagsakId,
            )

        assertThat(utbetalPåNyttFagområde).isFalse
        verify(exactly = 0) { fagsakService.settUtbetalPåNyttFagområde(any(), any()) }
    }

    private fun fagsak(utbetalPåNyttFagområde: Boolean?): Fagsak =
        mockk {
            every { this@mockk.utbetalPåNyttFagområde } returns utbetalPåNyttFagområde
        }
}
