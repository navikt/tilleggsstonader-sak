package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import org.springframework.data.annotation.Id
import java.util.UUID

data class IverksettingLogg(
    @Id
    val id: Long = 0,
    val iverksettingId: UUID,
    val utbetalingJson: JsonWrapper,
)
