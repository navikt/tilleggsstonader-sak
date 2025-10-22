package no.nav.tilleggsstonader.sak.utbetaling.id

import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
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
    val id: String = genererUtbetalingId(),
    val fagsakId: FagsakId,
    val typeAndel: TypeAndel,
) {
    init {
        feilHvis(id.length > 25) {
            "UtbetalingId må være kortere eller lik 25 tegn da økonomi ikke takler lengre id'er"
        }
    }

    companion object {
        private val prefix = "TS"

        fun genererUtbetalingId(): String =
            prefix +
                UUID
                    .randomUUID()
                    .toString()
                    .replace("-", "")
                    .take(25 - prefix.length)
    }
}
