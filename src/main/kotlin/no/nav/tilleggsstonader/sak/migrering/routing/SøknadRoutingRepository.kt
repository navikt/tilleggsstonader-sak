package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface SøknadRoutingRepository :
    RepositoryInterface<SøknadRouting, UUID>,
    InsertUpdateRepository<SøknadRouting> {
    fun findByIdentAndType(
        ident: String,
        type: SøknadsType,
    ): SøknadRouting?

    fun countByType(type: SøknadsType): Int
}

@Table("soknad_routing")
data class SøknadRouting(
    @Id
    val id: UUID = UUID.randomUUID(),
    val ident: String,
    val type: SøknadsType,
    val detaljer: JsonWrapper,
    val opprettetTid: LocalDateTime = SporbarUtils.now(),
)
