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
    val utbetalingId: String = genererUtbetalingId(),
    val fagsakId: FagsakId,
    val typeAndel: TypeAndel,
) {
    init {
        feilHvis(utbetalingId.length > 25) {
            "UtbetalingId må være kortere eller lik 25 tegn da økonomi ikke takler lengre id'er"
        }
    }

    companion object {
        private const val PREFIX = "TS"

        fun genererUtbetalingId(): String =
            PREFIX +
                randomUUID()
                    .toString()
                    .replace("-", "")
                    .take(25 - PREFIX.length)
    }
}
