import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import java.time.LocalDateTime

data class BehandlingTilJournalføringDto(
    val id: BehandlingId,
    val type: BehandlingType,
    val status: BehandlingStatus,
    val resultat: BehandlingResultat,
    val behandlingsÅrsak: BehandlingÅrsak,
    val sistEndret: LocalDateTime,
)
