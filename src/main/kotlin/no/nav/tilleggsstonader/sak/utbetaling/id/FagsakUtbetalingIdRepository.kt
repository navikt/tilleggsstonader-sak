package no.nav.tilleggsstonader.sak.utbetaling.id

import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import org.springframework.data.annotation.Id
import org.springframework.stereotype.Repository
import java.util.UUID
import java.util.UUID.randomUUID

@Repository
interface FagsakUtbetalingIdRepository :
    RepositoryInterface<FagsakUtbetalingId, UUID>,
    InsertUpdateRepository<FagsakUtbetalingId> {
    fun findByFagsakIdAndTypeAndel(
        fagsakId: FagsakId,
        typeAndel: TypeAndel,
    ): FagsakUtbetalingId?
}

data class FagsakUtbetalingId(
    @Id
    val utbetalingId: UUID = randomUUID(),
    val fagsakId: FagsakId,
    val typeAndel: TypeAndel,
)
