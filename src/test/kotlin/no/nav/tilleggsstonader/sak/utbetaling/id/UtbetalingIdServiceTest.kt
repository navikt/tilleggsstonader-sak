package no.nav.tilleggsstonader.sak.utbetaling.id

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UtbetalingIdServiceTest {
    private val fagsakUtbetalingIdRepository = mockk<FagsakUtbetalingIdRepository>(relaxed = true)
    private val fagsakUtbetalingIdService = FagsakUtbetalingIdService(fagsakUtbetalingIdRepository)
    private val fagsakId = FagsakId.random()
    private val reiseId = ReiseId.random()

    @Test
    fun `utbetalingId finnes for gitt fagsakId, typeAndel og reiseId, hentes`() {
        val typeAndel = TypeAndel.DAGLIG_REISE_AAP
        val fagsakUtbetalingId =
            FagsakUtbetalingId(
                fagsakId = fagsakId,
                typeAndel = typeAndel,
                reiseId = reiseId,
            )
        every { fagsakUtbetalingIdRepository.findByFagsakIdAndTypeAndelAndReiseId(fagsakId, typeAndel, reiseId) } returns fagsakUtbetalingId

        assertThat(
            fagsakUtbetalingIdService.hentEllerOpprettUtbetalingId(
                fagsakId = fagsakId,
                typeAndel = typeAndel,
                reiseId = reiseId,
            ),
        ).isEqualTo(fagsakUtbetalingId)

        verify(exactly = 0) { fagsakUtbetalingIdRepository.insert(any()) }
    }

    @Test
    fun `utbetalingId finnes ikke, oppretter ny`() {
        val typeAndel = TypeAndel.BOUTGIFTER_AAP
        every { fagsakUtbetalingIdRepository.findByFagsakIdAndTypeAndelAndReiseId(fagsakId, typeAndel, null) } returns null
        every { fagsakUtbetalingIdRepository.insert(any()) } answers { firstArg() }

        val utbetalingId =
            fagsakUtbetalingIdService.hentEllerOpprettUtbetalingId(
                fagsakId = fagsakId,
                typeAndel = typeAndel,
                reiseId = null,
            )

        assertThat(utbetalingId.fagsakId).isEqualTo(fagsakId)
        assertThat(utbetalingId.typeAndel).isEqualTo(typeAndel)
        assertThat(utbetalingId.reiseId).isNull()
        verify(exactly = 1) { fagsakUtbetalingIdRepository.insert(any()) }
    }
}
