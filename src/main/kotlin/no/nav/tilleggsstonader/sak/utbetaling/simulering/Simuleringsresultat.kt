package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table

/**
 * @property ingenEndringIUtbetaling: Hvis man simulerer med identiske utbetalinger som tidligere vil utsjekk svare med 204 NO CONTENT,
 * fordi det ikke er noen utbetalinger å simulere. Vi setter denne til true for å indikere at det ikke er noen endring i utbetaling.
 */
@Table
data class Simuleringsresultat(
    @Id
    val behandlingId: BehandlingId,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    val data: SimuleringResponse?,
    val ingenEndringIUtbetaling: Boolean = false,
)
