package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.HenlagtÅrsak
import no.nav.tilleggsstonader.sak.behandling.dto.BehandlingDto
import no.nav.tilleggsstonader.sak.behandling.dto.HenlagtDto
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.util.ProblemDetailUtil.catchProblemDetailException
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.exchange
import java.util.UUID

internal class BehandlingControllerTest : IntegrationTest() {

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(onBehalfOfToken())
    }

    @Test
    internal fun `Skal returnere 403 dersom man ikke har tilgang til brukeren`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("ikkeTilgang"))))
        val behandling = testoppsettService.lagre(behandling(fagsak))
        val respons = catchThrowableOfType<HttpClientErrorException.Forbidden> { hentBehandling(behandling.id) }

        assertThat(respons.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    internal fun `Skal henlegge behandling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("12345678901"))))
        val behandling = testoppsettService.lagre(behandling(fagsak, type = BehandlingType.FØRSTEGANGSBEHANDLING))
        val respons = henlegg(behandling.id, HenlagtDto(årsak = HenlagtÅrsak.FEILREGISTRERT))

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body!!.resultat).isEqualTo(BehandlingResultat.HENLAGT)
        assertThat(respons.body!!.henlagtÅrsak).isEqualTo(HenlagtÅrsak.FEILREGISTRERT)
    }

    @Test
    internal fun `Skal henlegge FØRSTEGANGSBEHANDLING`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("12345678901"))))
        val behandling = testoppsettService.lagre(behandling(fagsak, type = BehandlingType.FØRSTEGANGSBEHANDLING))
        val respons = henlegg(behandling.id, HenlagtDto(årsak = HenlagtÅrsak.FEILREGISTRERT))

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body!!.resultat).isEqualTo(BehandlingResultat.HENLAGT)
        assertThat(respons.body!!.henlagtÅrsak).isEqualTo(HenlagtÅrsak.FEILREGISTRERT)
    }

    @Nested
    inner class OpprettelseAvGrunnlagsdata {
        val behandling = behandling()

        @BeforeEach
        fun setUp() {
            testoppsettService.opprettBehandlingMedFagsak(behandling, opprettGrunnlagsdata = false)
        }

        @Test
        fun `behandlingStatus=OPPRETTET og veileder skal ikke hentes då det opprettes grunnlag`() {
            headers.setBearerAuth(onBehalfOfToken(role = rolleConfig.veilederRolle))

            val exception = catchProblemDetailException { hentBehandling(behandling.id) }
            assertThat(exception.detail.detail).contains("Behandlingen er ikke påbegynt")
        }

        @Test
        fun `behandlingStatus=UTREDES og veilder skal kunne hente behandlingen hvis statusen er annet enn UTREDES`() {
            testoppsettService.oppdater(behandling.copy(status = BehandlingStatus.UTREDES))
            headers.setBearerAuth(onBehalfOfToken(role = rolleConfig.veilederRolle))

            hentBehandling(behandling.id)
        }

        @Test
        fun `behandlingStatus=OPPRETTET skal kunne opprette grunnlag hvis bruker er saksbehandler`() {
            hentBehandling(behandling.id)
        }
    }

    private fun hentBehandling(id: UUID): ResponseEntity<BehandlingDto> {
        return restTemplate.exchange(
            localhost("/api/behandling/$id"),
            HttpMethod.GET,
            HttpEntity<BehandlingDto>(headers),
        )
    }

    private fun henlegg(id: UUID, henlagt: HenlagtDto): ResponseEntity<BehandlingDto> {
        return restTemplate.exchange<BehandlingDto>(
            localhost("/api/behandling/$id/henlegg"),
            HttpMethod.POST,
            HttpEntity(henlagt, headers),
        )
    }
}
