package no.nav.tilleggsstonader.sak.utbetaling.migrering

import no.nav.tilleggsstonader.libs.http.client.postForEntityNullable
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.UtbetalingId
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class MigreringClient(
    @Value("\${clients.simulering.uri}") private val uri: URI,
    @Qualifier("azure") private val restTemplate: RestTemplate,
) {
    private val migreringUrl =
        UriComponentsBuilder
            .fromUri(uri)
            .pathSegment("api", "iverksetting", "v2", "migrate")
            .encode()
            .toUriString()

    fun migrer(dto: MigrerUtbetalingDto) {
        restTemplate.postForEntityNullable<Void>(migreringUrl, dto)
    }
}

data class MigrerUtbetalingDto(
    val sakId: String,
    val behandlingId: String,
    val iverksettingId: String?,
    val meldeperiode: String?,
    val uidToStønad: Pair<UtbetalingId, StønadstypeIverksetting>,
)

enum class StønadstypeIverksetting {
    TILSYN_BARN_ENSLIG_FORSØRGER,
    TILSYN_BARN_AAP,
    TILSYN_BARN_ETTERLATTE,

    LÆREMIDLER_ENSLIG_FORSØRGER,
    LÆREMIDLER_AAP,
    LÆREMIDLER_ETTERLATTE,

    BOUTGIFTER_ENSLIG_FORSØRGER,
    BOUTGIFTER_AAP,
    BOUTGIFTER_ETTERLATTE,
}
