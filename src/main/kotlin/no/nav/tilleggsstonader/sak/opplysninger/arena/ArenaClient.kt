package no.nav.tilleggsstonader.sak.opplysninger.arena

import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusHarSakerDto
import no.nav.tilleggsstonader.kontrakter.arena.oppgave.ArenaOppgaveDto
import no.nav.tilleggsstonader.kontrakter.arena.vedtak.ArenaSakOgVedtakDto
import no.nav.tilleggsstonader.kontrakter.felles.IdenterRequest
import no.nav.tilleggsstonader.kontrakter.felles.IdenterStønadstype
import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class ArenaClient(
    @Value("\${clients.arena.uri}") private val arenaUri: URI,
    @Qualifier("azureClientCredential") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate) {
    val statusUri = UriComponentsBuilder.fromUri(arenaUri).pathSegment("api", "status").toUriString()

    val vedtakUri = UriComponentsBuilder.fromUri(arenaUri).pathSegment("api", "vedtak").toUriString()

    val statusHarSakerUri = UriComponentsBuilder.fromUri(arenaUri).pathSegment("api", "status", "har-saker").toUriString()

    val oppgaverUri = UriComponentsBuilder.fromUri(arenaUri).pathSegment("api", "oppgave").toUriString()

    fun hentStatus(request: IdenterStønadstype): ArenaStatusDto = postForEntity(statusUri, request)

    fun hentVedtak(request: IdenterRequest): ArenaSakOgVedtakDto = postForEntity(vedtakUri, request)

    fun harSaker(request: IdenterRequest): ArenaStatusHarSakerDto = postForEntity(statusHarSakerUri, request)

    fun hentOppgaver(request: IdenterRequest): List<ArenaOppgaveDto> = postForEntity(oppgaverUri, request)
}
