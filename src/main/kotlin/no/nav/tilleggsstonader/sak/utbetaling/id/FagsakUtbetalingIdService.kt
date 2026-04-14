package no.nav.tilleggsstonader.sak.utbetaling.id

import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import org.springframework.stereotype.Service

@Service
class FagsakUtbetalingIdService(
    private val fagsakUtbetalingIdRepository: FagsakUtbetalingIdRepository,
) {
    fun hentEllerOpprettUtbetalingId(
        fagsakId: FagsakId,
        typeAndel: TypeAndel,
        reiseId: ReiseId?,
    ): FagsakUtbetalingId {
        val eksisterende = fagsakUtbetalingIdRepository.findByFagsakIdAndTypeAndelAndReiseId(fagsakId, typeAndel, reiseId)
        if (eksisterende != null) {
            return eksisterende
        }

        return fagsakUtbetalingIdRepository.insert(
            FagsakUtbetalingId(
                fagsakId = fagsakId,
                typeAndel = typeAndel,
                reiseId = reiseId,
            ),
        )
    }

    fun finnesUtbetalingsId(
        fagsakId: FagsakId,
        typeAndel: TypeAndel,
        reiseId: ReiseId?,
    ): Boolean = fagsakUtbetalingIdRepository.findByFagsakIdAndTypeAndelAndReiseId(fagsakId, typeAndel, reiseId) != null

    fun hentUtbetalingIderForFagsakId(fagsakId: FagsakId): List<FagsakUtbetalingId> = fagsakUtbetalingIdRepository.findByFagsakId(fagsakId)
}
