package no.nav.tilleggsstonader.sak.klage

import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.klage.KlagebehandlingDto
import no.nav.tilleggsstonader.kontrakter.klage.OppgaverBehandlingerRequest
import no.nav.tilleggsstonader.kontrakter.klage.OppgaverBehandlingerResponse
import no.nav.tilleggsstonader.kontrakter.klage.OpprettKlagebehandlingRequest
import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.UUID

@Component
class KlageClient(
    @Qualifier("azure") restTemplate: RestTemplate,
    @Value("\${clients.klage.uri}") private val klageUri: URI,
) : AbstractRestClient(restTemplate) {
    private val opprettKlage =
        UriComponentsBuilder
            .fromUri(klageUri)
            .path("api/ekstern/behandling/opprett")
            .build()
            .toUriString()

    fun opprettKlage(opprettKlagebehandlingRequest: OpprettKlagebehandlingRequest) {
        postForEntityNullable<Void>(opprettKlage, opprettKlagebehandlingRequest)
    }

    fun hentKlagebehandlinger(eksternIder: Set<Long>): Map<Long, List<KlagebehandlingDto>> {
        val uri =
            UriComponentsBuilder
                .fromUri(klageUri)
                .path("api/ekstern/behandling/{fagsystem}")
                .queryParam("eksternFagsakId", "{eksternFagsakId}")
                .encode()
                .toUriString()

        return getForEntity<Map<Long, List<KlagebehandlingDto>>>(
            uri = uri,
            uriVariables =
                mapOf(
                    "fagsystem" to Fagsystem.TILLEGGSSTONADER,
                    "eksternFagsakId" to eksternIder.joinToString(","),
                ),
        )
    }

    fun hentBehandlingerForOppgaveIder(oppgaveIder: List<Long>): Map<Long, UUID> {
        val uri =
            UriComponentsBuilder
                .fromUri(klageUri)
                .path("api/ekstern/behandling/finn-oppgaver")
                .build()
                .toUriString()
        val request = OppgaverBehandlingerRequest(oppgaveIder)
        return postForEntity<OppgaverBehandlingerResponse>(uri, request).oppgaver
    }
}
