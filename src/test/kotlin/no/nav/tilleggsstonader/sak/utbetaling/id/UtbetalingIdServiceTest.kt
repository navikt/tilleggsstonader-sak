package no.nav.tilleggsstonader.sak.utbetaling.id

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UtbetalingIdServiceTest {
    private val fagsakUtbetalingIdRepository = mockk<FagsakUtbetalingIdRepository>()
    private val fagsakUtbetalingIdService = FagsakUtbetalingIdService(fagsakUtbetalingIdRepository)

    val fagsakId = FagsakId.random()
    val typeAndel = TypeAndel.DAGLIG_REISE_AAP
    val reiseId = ReiseId.random()

    @Test
    fun `utbetalingId finnes ikke for gitt fagsakId, typeAndel og reiseId, opprettes`() {
        every { fagsakUtbetalingIdRepository.findByFagsakIdAndTypeAndelAndReiseId(fagsakId, typeAndel, reiseId) } returns null
        every { fagsakUtbetalingIdRepository.insert(any()) } answers { firstArg() }
        val utbetalingId = fagsakUtbetalingIdService.hentEllerOpprettUtbetalingId(fagsakId, typeAndel, reiseId)

        verify { fagsakUtbetalingIdRepository.insert(utbetalingId) }
    }

    @Test
    fun `utbetalingId finnes for gitt fagsakId, typeAndel og reiseId, hentes`() {
        val fagsakUtbetalingId = FagsakUtbetalingId(fagsakId = fagsakId, typeAndel = typeAndel, reiseId = reiseId)
        every { fagsakUtbetalingIdRepository.findByFagsakIdAndTypeAndelAndReiseId(fagsakId, typeAndel, reiseId) } returns fagsakUtbetalingId

        assertThat(fagsakUtbetalingIdService.hentEllerOpprettUtbetalingId(fagsakId, typeAndel, reiseId)).isEqualTo(fagsakUtbetalingId)
        verify(exactly = 0) { fagsakUtbetalingIdRepository.insert(any()) }
    }
}
