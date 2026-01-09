package no.nav.tilleggsstonader.sak.utbetaling.id

import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import org.springframework.stereotype.Service

@Service
class FagsakUtbetalingIdService(
    private val fagsakUtbetalingIdRepository: FagsakUtbetalingIdRepository,
) {
    fun hentEllerOpprettUtbetalingId(
        fagsakId: FagsakId,
        typeAndel: TypeAndel,
    ): FagsakUtbetalingId {
        val eksisterende = fagsakUtbetalingIdRepository.findByFagsakIdAndTypeAndel(fagsakId, typeAndel)
        if (eksisterende != null) {
            return eksisterende
        }

        return fagsakUtbetalingIdRepository.insert(
            FagsakUtbetalingId(
                fagsakId = fagsakId,
                typeAndel = typeAndel,
            ),
        )
    }

    fun finnesUtbetalingsId(
        fagsakId: FagsakId,
        typeAndel: TypeAndel,
    ): Boolean = fagsakUtbetalingIdRepository.findByFagsakIdAndTypeAndel(fagsakId, typeAndel) != null
}
