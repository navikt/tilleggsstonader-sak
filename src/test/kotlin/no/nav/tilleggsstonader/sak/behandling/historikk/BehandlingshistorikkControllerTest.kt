package no.nav.tilleggsstonader.sak.behandling.historikk

import no.nav.security.mock.oauth2.http.objectMapper
import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.Behandlingshistorikk
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.BehandlingshistorikkRepository
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.behandling.historikk.dto.HendelseshistorikkDto
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.exchange
import java.time.LocalDateTime
import java.util.UUID

internal class BehandlingshistorikkControllerTest : IntegrationTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingshistorikkRepository: BehandlingshistorikkRepository

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(onBehalfOfToken())
    }

    @Test
    internal fun `Skal returnere 403 dersom man ikke har tilgang til brukeren`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("ikkeTilgang"))))
        val behandling = testoppsettService.lagre(behandling(fagsak))
        val respons = catchThrowableOfType<HttpClientErrorException.Forbidden> { hentHistorikk(behandling.id) }

        assertThat(respons.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    internal fun `skal kun returnere den første hendelsen av typen OPPRETTET - etterfølgende hendelser av denne typen skal lukes vekk`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(""))))
        val behandling = testoppsettService.lagre(behandling(fagsak))

        leggInnHistorikk(behandling, "1", osloNow(), StegType.INNGANGSVILKÅR)
        leggInnHistorikk(behandling, "2", osloNow().minusDays(1), StegType.INNGANGSVILKÅR)
        leggInnHistorikk(behandling, "3", osloNow().plusDays(1), StegType.INNGANGSVILKÅR)

        val respons = hentHistorikk(behandling.id)
        assertThat(respons.body!!.map { it.endretAvNavn }).containsExactly("2")
    }

    @Test
    internal fun `skal returnere hendelser av alle typer i riktig rekkefølge for invilget behandling `() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(""))))
        val behandling = testoppsettService.lagre(behandling(fagsak))

        leggInnHistorikk(behandling, "1", osloNow(), StegType.INNGANGSVILKÅR)
        leggInnHistorikk(behandling, "2", osloNow(), StegType.VILKÅR)
        leggInnHistorikk(behandling, "3", osloNow().plusDays(1), StegType.BEREGNE_YTELSE)
        leggInnHistorikk(behandling, "4", osloNow().plusDays(2), StegType.SEND_TIL_BESLUTTER)
        leggInnHistorikk(
            behandling,
            "5",
            osloNow().plusDays(3),
            StegType.BESLUTTE_VEDTAK,
            stegUtfall = StegUtfall.BESLUTTE_VEDTAK_GODKJENT,
        )
        leggInnHistorikk(behandling, "6", osloNow().plusDays(4), StegType.JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV)
        // leggInnHistorikk(behandling, "6", osloNow().plusDays(5), StegType.LAG_SAKSBEHANDLINGSBLANKETT)
        leggInnHistorikk(behandling, "7", osloNow().plusDays(6), StegType.FERDIGSTILLE_BEHANDLING)
        leggInnHistorikk(behandling, "8", osloNow().plusDays(7), StegType.BEHANDLING_FERDIGSTILT)
        behandlingRepository.update(
            behandling.copy(
                resultat = BehandlingResultat.INNVILGET,
                vedtakstidspunkt = SporbarUtils.now(),
            ),
        )
        val respons = hentHistorikk(behandling.id)
        assertThat(respons.body!!.map { it.endretAvNavn }).containsExactly("7", "5", "4", "1")
    }

    @Test
    internal fun `skal returnere hendelser av alle typer i riktig rekkefølge for henlagt behandling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(""))))
        val behandling = testoppsettService.lagre(behandling(fagsak))

        leggInnHistorikk(behandling, "1", osloNow(), StegType.INNGANGSVILKÅR)
        leggInnHistorikk(behandling, "2", osloNow(), StegType.VILKÅR)
        leggInnHistorikk(behandling, "3", osloNow().plusDays(1), StegType.BEREGNE_YTELSE)
        leggInnHistorikk(behandling, "4", osloNow().plusDays(2), StegType.SEND_TIL_BESLUTTER)
        leggInnHistorikk(
            behandling,
            "5",
            osloNow().plusDays(3),
            StegType.BESLUTTE_VEDTAK,
            stegUtfall = StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT,
        )
        leggInnHistorikk(behandling, "6", osloNow().plusDays(6), StegType.FERDIGSTILLE_BEHANDLING)
        leggInnHistorikk(behandling, "7", osloNow().plusDays(8), StegType.BEHANDLING_FERDIGSTILT)
        behandlingRepository.update(
            behandling.copy(
                resultat = BehandlingResultat.HENLAGT,
                vedtakstidspunkt = SporbarUtils.now(),
            ),
        )
        val respons = hentHistorikk(behandling.id)
        assertThat(respons.body!!.map { it.endretAvNavn }).containsExactly("7", "5", "4", "1")
    }

    @Test
    internal fun `skal returnere alle hendelser dersom en behandling blir underkjent i totrinnskontroll, deretter sendt til beslutter på nytt og deretter godkjent`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(""))))
        val behandling = testoppsettService.lagre(behandling(fagsak))

        leggInnHistorikk(behandling, "1", osloNow(), StegType.INNGANGSVILKÅR)
        leggInnHistorikk(behandling, "2", osloNow(), StegType.VILKÅR)
        leggInnHistorikk(behandling, "3", osloNow().plusDays(1), StegType.BEREGNE_YTELSE)
        leggInnHistorikk(behandling, "4", osloNow().plusDays(2), StegType.SEND_TIL_BESLUTTER)
        leggInnHistorikk(
            behandling,
            "5",
            osloNow().plusDays(3),
            StegType.BESLUTTE_VEDTAK,
            stegUtfall = StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT,
        )
        leggInnHistorikk(behandling, "6", osloNow().plusDays(4), StegType.SEND_TIL_BESLUTTER)
        leggInnHistorikk(
            behandling,
            "7",
            osloNow().plusDays(5),
            StegType.BESLUTTE_VEDTAK,
            stegUtfall = StegUtfall.BESLUTTE_VEDTAK_GODKJENT,
        )

        val respons = hentHistorikk(behandling.id)
        assertThat(respons.body!!.map { it.endretAvNavn }).containsExactly("7", "6", "5", "4", "1")
    }

    @Test
    internal fun `skal returnere metadata som json`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(""))))
        val behandling = testoppsettService.lagre(behandling(fagsak))

        val jsonMap = mapOf("key" to "value")
        val metadata = JsonWrapper(objectMapper.writeValueAsString(jsonMap))
        behandlingshistorikkRepository.insert(
            Behandlingshistorikk(
                behandlingId = behandling.id,
                steg = behandling.steg,
                metadata = metadata,
            ),
        )

        val respons = hentHistorikk(behandling.id)
        assertThat(respons.body?.first()?.metadata).isEqualTo(jsonMap)
    }

    private fun leggInnHistorikk(
        behandling: Behandling,
        opprettetAv: String,
        endretTid: LocalDateTime,
        steg: StegType? = null,
        stegUtfall: StegUtfall? = null,
    ) {
        behandlingshistorikkRepository.insert(
            Behandlingshistorikk(
                behandlingId = behandling.id,
                steg = steg ?: behandling.steg,
                utfall = stegUtfall,
                opprettetAv = opprettetAv,
                opprettetAvNavn = opprettetAv,
                endretTid = endretTid,
            ),
        )
    }

    private fun hentHistorikk(id: UUID): ResponseEntity<List<HendelseshistorikkDto>> {
        return restTemplate.exchange(
            localhost("/api/behandlingshistorikk/$id"),
            HttpMethod.GET,
            HttpEntity(null, headers),
        )
    }
}
