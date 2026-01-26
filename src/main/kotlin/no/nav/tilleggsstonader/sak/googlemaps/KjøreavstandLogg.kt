package no.nav.tilleggsstonader.sak.googlemaps

import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table("kjoreavstand_logg")
data class KjøreavstandLogg(
    @Id
    val id: UUID = UUID.randomUUID(),
    val tidspunkt: LocalDateTime = SporbarUtils.now(),
    val saksbehandler: String = SikkerhetContext.hentSaksbehandler(),
    val sporring: JsonWrapper,
    val resultat: JsonWrapper?,
)
