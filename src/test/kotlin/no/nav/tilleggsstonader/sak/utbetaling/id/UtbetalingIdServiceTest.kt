package no.nav.tilleggsstonader.sak.utbetaling.id

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UtbetalingIdServiceTest {
    private val fagsakUtbetalingIdRepository = mockk<FagsakUtbetalingIdRepository>()
    private val fagsakUtbetalingIdService = FagsakUtbetalingIdService(fagsakUtbetalingIdRepository)

    val fagsakId = FagsakId.random()
    val typeAndel = TypeAndel.DAGLIG_REISE_AAP

    @Test
    fun `utbetalingId finnes ikke for gitt fagsakId og typeAndel, opprettes`() {
        every { fagsakUtbetalingIdRepository.findByFagsakIdAndTypeAndel(fagsakId, typeAndel) } returns null
        every { fagsakUtbetalingIdRepository.insert(any()) } answers { firstArg() }
        val utbetalingId = fagsakUtbetalingIdService.hentEllerOpprettUtbetalingId(fagsakId, typeAndel)

        verify { fagsakUtbetalingIdRepository.insert(utbetalingId) }
    }

    @Test
    fun `utbetalingId finnes for gitt fagsakId og typeAndel, hentes`() {
        val fagsakUtbetalingId = FagsakUtbetalingId(fagsakId = fagsakId, typeAndel = typeAndel)
        every { fagsakUtbetalingIdRepository.findByFagsakIdAndTypeAndel(fagsakId, typeAndel) } returns fagsakUtbetalingId

        assertThat(fagsakUtbetalingIdService.hentEllerOpprettUtbetalingId(fagsakId, typeAndel)).isEqualTo(fagsakUtbetalingId)
        verify(exactly = 0) { fagsakUtbetalingIdRepository.insert(any()) }
    }
}
