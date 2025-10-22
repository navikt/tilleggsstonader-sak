package no.nav.tilleggsstonader.sak.utbetaling.id

import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import org.springframework.stereotype.Service

@Service
class UtbetalingIdService(
    private val utbetalingIdRepository: UtbetalingIdRepository,
) {
    fun hentEllerOpprettUtbetalingId(
        fagsakId: FagsakId,
        typeAndel: TypeAndel,
    ): UtbetalingId {
        val eksisterende = utbetalingIdRepository.findByFagsakIdAndTypeAndel(fagsakId, typeAndel)
        if (eksisterende != null) {
            return eksisterende
        }

        return utbetalingIdRepository.insert(
            UtbetalingId(
                fagsakId = fagsakId,
                typeAndel = typeAndel,
            ),
        )
    }
}
