package no.nav.tilleggsstonader.sak.utbetaling.id

import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.UtbetalingId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import org.springframework.data.annotation.Id
import org.springframework.stereotype.Repository

@Repository
interface FagsakUtbetalingIdRepository :
    RepositoryInterface<FagsakUtbetalingId, UtbetalingId>,
    InsertUpdateRepository<FagsakUtbetalingId> {
    fun findByFagsakIdAndTypeAndelAndReiseId(
        fagsakId: FagsakId,
        typeAndel: TypeAndel,
        reiseId: ReiseId?,
    ): FagsakUtbetalingId?

    fun findByFagsakId(fagsakId: FagsakId): List<FagsakUtbetalingId>
}

data class FagsakUtbetalingId(
    @Id
    val utbetalingId: UtbetalingId = UtbetalingId.random(),
    val fagsakId: FagsakId,
    val typeAndel: TypeAndel,
    val reiseId: ReiseId?,
)
