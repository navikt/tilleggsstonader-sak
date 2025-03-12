package no.nav.tilleggsstonader.sak.hendelser

import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.data.annotation.Id
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface HendelseRepository :
    RepositoryInterface<Hendelse, String>,
    InsertUpdateRepository<Hendelse> {
    fun existsByTypeAndId(
        type: TypeHendelse,
        id: String,
    ): Boolean
}

/**
 * Primary key er [id] og [type] sammen, men spring-jdbc håndterer ikke composite key
 * Samtidig trenger man annotere et felt med [@Id]
 */
data class Hendelse(
    val type: TypeHendelse,
    @Id
    val id: String,
    val opprettetTid: LocalDateTime = SporbarUtils.now(),
)

enum class TypeHendelse {
    JOURNALPOST,
}
