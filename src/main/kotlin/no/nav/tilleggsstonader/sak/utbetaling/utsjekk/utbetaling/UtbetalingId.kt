package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import java.util.UUID

/**
 * Utbetaling er en UUID som konsumenter av Utsjekk har ansvar for å lage og holde styr
 * på i egen løsning. Denne brukes til å unikt identifisere utbetalingen og kan brukes
 * når man evt. ønsker å gjøre endringer eller opphør på en utbetaling.
 *
 */
@JvmInline
value class UtbetalingId(
    val id: UUID,
) {
    override fun toString(): String = id.toString()

    companion object {
        fun random() = UtbetalingId(UUID.randomUUID())

        fun fromString(id: String) = BarnId(UUID.fromString(id))
    }
}
