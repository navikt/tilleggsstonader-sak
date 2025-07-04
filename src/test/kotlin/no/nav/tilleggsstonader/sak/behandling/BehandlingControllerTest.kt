package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
import no.nav.tilleggsstonader.libs.test.httpclient.ProblemDetailUtil.catchProblemDetailException
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingId
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingIdRepository
import no.nav.tilleggsstonader.sak.behandling.domain.HenlagtÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.NyeOpplysningerEndring
import no.nav.tilleggsstonader.sak.behandling.domain.NyeOpplysningerKilde
import no.nav.tilleggsstonader.sak.behandling.domain.NyeOpplysningerMetadata
import no.nav.tilleggsstonader.sak.behandling.dto.BehandlingDto
import no.nav.tilleggsstonader.sak.behandling.dto.HenlagtDto
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.exchange
import java.time.LocalDate

internal class BehandlingControllerTest : IntegrationTest() {
    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var eksternBehandlingIdRepository: EksternBehandlingIdRepository

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
        val henlagtBegrunnelse = "Registert feil"
        val respons =
            henlegg(behandling.id, HenlagtDto(årsak = HenlagtÅrsak.FEILREGISTRERT, begrunnelse = henlagtBegrunnelse))

        assertThat(respons.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        val oppdatert = behandlingRepository.findByIdOrThrow(behandling.id)
        assertThat(oppdatert.resultat).isEqualTo(BehandlingResultat.HENLAGT)
        assertThat(oppdatert.henlagtÅrsak).isEqualTo(HenlagtÅrsak.FEILREGISTRERT)
        assertThat(oppdatert.henlagtBegrunnelse).isEqualTo(henlagtBegrunnelse)
    }

    @Test
    internal fun `Skal mappe eksternId til behandlingsId`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("12345678901"))))
        val behandling = testoppsettService.lagre(behandling(fagsak, type = BehandlingType.FØRSTEGANGSBEHANDLING))
        val eskternBehandlingId =
            eksternBehandlingIdRepository.insert(
                EksternBehandlingId(fagsak.eksternId.id, BehandlingId(behandling.id.id)),
            )

        val respons = hentBehandlingByEksternId(eskternBehandlingId.id)

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)

        val oppdatert = eksternBehandlingIdRepository.findByIdOrThrow(eskternBehandlingId.id)
        assertThat(oppdatert.id).isEqualTo(eskternBehandlingId.id)
        assertThat(oppdatert.behandlingId.id).isEqualTo(respons.body!!.id)
    }

    @Test
    internal fun `Skal sette revurder fra dato`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("12345678901"))))
        val førstegangsbehandling =
            testoppsettService.lagre(behandling(fagsak, type = BehandlingType.FØRSTEGANGSBEHANDLING))
        testoppsettService.ferdigstillBehandling(førstegangsbehandling)

        val revurdering =
            testoppsettService.lagre(
                behandling(
                    fagsak,
                    type = BehandlingType.REVURDERING,
                    forrigeIverksatteBehandlingId = førstegangsbehandling.id,
                ),
            )

        val revurderFraDato = LocalDate.of(2025, 1, 1)
        val respons = revurderFra(revurdering.id, revurderFraDato)

        assertThat(respons.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        val oppdatert = behandlingRepository.findByIdOrThrow(revurdering.id)
        assertThat(oppdatert.revurderFra).isEqualTo(revurderFraDato)
    }

    @Test
    internal fun `Skal henlegge FØRSTEGANGSBEHANDLING`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("12345678901"))))
        val behandling = testoppsettService.lagre(behandling(fagsak, type = BehandlingType.FØRSTEGANGSBEHANDLING))
        val henlagtBegrunnelse = "Registert feil"
        val respons =
            henlegg(behandling.id, HenlagtDto(årsak = HenlagtÅrsak.FEILREGISTRERT, begrunnelse = henlagtBegrunnelse))

        assertThat(respons.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        val oppdatert = behandlingRepository.findByIdOrThrow(behandling.id)

        assertThat(oppdatert.resultat).isEqualTo(BehandlingResultat.HENLAGT)
        assertThat(oppdatert.henlagtÅrsak).isEqualTo(HenlagtÅrsak.FEILREGISTRERT)
        assertThat(oppdatert.henlagtBegrunnelse).isEqualTo(henlagtBegrunnelse)
    }

    @Test
    fun `viser nyeOpplysningerMetadata på behandling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("12345678901"))))
        val behandling =
            testoppsettService.lagre(
                behandling(fagsak, type = BehandlingType.FØRSTEGANGSBEHANDLING).copy(
                    årsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                    nyeOpplysningerMetadata =
                        NyeOpplysningerMetadata(
                            kilde = NyeOpplysningerKilde.ETTERSENDING,
                            endringer = listOf(NyeOpplysningerEndring.MÅLGRUPPE),
                            beskrivelse = "Hello world",
                        ),
                ),
            )
        val response = hentBehandling(behandling.id)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.nyeOpplysningerMetadata).isNotNull
        assertThat(response.body?.nyeOpplysningerMetadata?.kilde).isEqualTo(NyeOpplysningerKilde.ETTERSENDING)
        assertThat(response.body?.nyeOpplysningerMetadata?.endringer).containsExactly(NyeOpplysningerEndring.MÅLGRUPPE)
        assertThat(response.body?.nyeOpplysningerMetadata?.beskrivelse).isEqualTo("Hello world")
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

    private fun hentBehandling(id: BehandlingId): ResponseEntity<BehandlingDto> =
        restTemplate.exchange(
            localhost("/api/behandling/$id"),
            HttpMethod.GET,
            HttpEntity<BehandlingDto>(headers),
        )

    private fun hentBehandlingByEksternId(eksternBehandlingId: Long): ResponseEntity<BehandlingId> =
        restTemplate.exchange(
            localhost("/api/behandling/ekstern/$eksternBehandlingId"),
            HttpMethod.GET,
            HttpEntity<BehandlingId>(headers),
        )

    private fun revurderFra(
        behandlingId: BehandlingId,
        revurderFra: LocalDate,
    ): ResponseEntity<Void> =
        restTemplate.exchange(
            localhost("/api/behandling/$behandlingId/revurder-fra/$revurderFra"),
            HttpMethod.POST,
            HttpEntity<Void>(null, headers),
            Void::class.java,
        )

    private fun henlegg(
        id: BehandlingId,
        henlagt: HenlagtDto,
    ): ResponseEntity<BehandlingDto> =
        restTemplate.exchange<BehandlingDto>(
            localhost("/api/behandling/$id/henlegg"),
            HttpMethod.POST,
            HttpEntity(henlagt, headers),
        )
}
