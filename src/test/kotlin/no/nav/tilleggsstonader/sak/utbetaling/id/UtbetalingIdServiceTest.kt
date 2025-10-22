package no.nav.tilleggsstonader.sak.utbetaling.id

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class UtbetalingIdServiceTest {
    private val utbetalingIdRepository = mockk<UtbetalingIdRepository>()
    private val utbetalingIdService = UtbetalingIdService(utbetalingIdRepository)

    val fagsakId = FagsakId.random()
    val typeAndel = TypeAndel.DAGLIG_REISE_AAP

    @Test
    fun `utbetalingId finnes ikke for gitt fagsakId og typeAndel, opprettes`() {
        every { utbetalingIdRepository.findByFagsakIdAndTypeAndel(fagsakId, typeAndel) } returns null
        every { utbetalingIdRepository.insert(any()) } answers { firstArg() }
        val utbetalingId = utbetalingIdService.hentEllerOpprettUtbetalingId(fagsakId, typeAndel)

        verify { utbetalingIdRepository.insert(utbetalingId) }
    }

    @Test
    fun `utbetalingId finnes for gitt fagsakId og typeAndel, hentes`() {
        val utbetalingId = UtbetalingId(fagsakId = fagsakId, typeAndel = typeAndel)
        every { utbetalingIdRepository.findByFagsakIdAndTypeAndel(fagsakId, typeAndel) } returns utbetalingId

        assertThat(utbetalingIdService.hentEllerOpprettUtbetalingId(fagsakId, typeAndel)).isEqualTo(utbetalingId)
        verify(exactly = 0) { utbetalingIdRepository.insert(any()) }
    }
}
