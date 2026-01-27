package no.nav.tilleggsstonader.sak.utbetaling.id

import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.UtbetalingId
import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository

@Repository
interface FagsakUtbetalingIdRepository :
    RepositoryInterface<FagsakUtbetalingId, UtbetalingId>,
    InsertUpdateRepository<FagsakUtbetalingId> {
    fun findByFagsakIdAndTypeAndel(
        fagsakId: FagsakId,
        typeAndel: TypeAndel,
    ): FagsakUtbetalingId?

    fun findByFagsakId(fagsakId: FagsakId): List<FagsakUtbetalingId>

    @Query("select id from fagsak where id not in (select fagsak_id from fagsak_utbetaling_id)")
    fun finnAlleFagsakerUtenUtbetalingId(): List<FagsakId>
}

data class FagsakUtbetalingId(
    @Id
    val utbetalingId: UtbetalingId = UtbetalingId.random(),
    val fagsakId: FagsakId,
    val typeAndel: TypeAndel,
)
