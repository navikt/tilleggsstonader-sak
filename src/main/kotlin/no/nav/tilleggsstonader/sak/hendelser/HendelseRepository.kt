package no.nav.tilleggsstonader.sak.hendelser

import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.data.annotation.Id
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Lagrer ned hendelser som er håndtert i tilfelle offset skulle bli resatt ved en uhell
 * og man prøver å lese samme hendelse på nytt
 *
 * Familie-enslig forsørger hadde et tilfelle der de ved en oppdatering av en avhengighet
 * resatte offset og leste alle hendelser på nytt
 */
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
    val metadata: JsonWrapper? = null,
    val opprettetTid: LocalDateTime = SporbarUtils.now(),
) {
    constructor(type: TypeHendelse, id: String, metadata: Map<String, Any>? = null) :
        this(
            type = type,
            id = id,
            metadata = metadata?.let { JsonWrapper(objectMapper.writeValueAsString(it)) },
        )
}

enum class TypeHendelse {
    JOURNALPOST,
}
