package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.tilleggsstonader.kontrakter.felles.Skjematype
import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface SkjemaRoutingRepository :
    RepositoryInterface<SkjemaRouting, UUID>,
    InsertUpdateRepository<SkjemaRouting> {
    fun findByIdentAndType(
        ident: String,
        type: Skjematype,
    ): SkjemaRouting?

    fun countByType(type: Skjematype): Int

    @Query(
        "SELECT COUNT(*) FROM skjema_routing WHERE type = :type AND detaljer::jsonb = :detaljer::jsonb",
    )
    fun countByTypeAndDetaljerContains(
        @Param("type") type: Skjematype,
        @Param("detaljer") detaljer: String,
    ): Int
}

data class SkjemaRouting(
    @Id
    val id: UUID = UUID.randomUUID(),
    val ident: String,
    val type: Skjematype,
    val detaljer: JsonWrapper,
    val opprettetTid: LocalDateTime = SporbarUtils.now(),
)
