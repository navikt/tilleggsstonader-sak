package no.nav.tilleggsstonader.sak.utbetaling.id

import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UtbetalingIdRepository :
    RepositoryInterface<UtbetalingId, UUID>,
    InsertUpdateRepository<UtbetalingId> {
    fun findByFagsakIdAndTypeAndel(
        fagsakId: FagsakId,
        typeAndel: TypeAndel,
    ): UtbetalingId?
}

data class UtbetalingId(
    val id: UUID,
    val fagsakId: FagsakId,
    val typeAndel: TypeAndel,
)
