package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
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
import no.nav.tilleggsstonader.sak.behandling.dto.HenlagtDto
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectProblemDetail
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

internal class BehandlingControllerTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var eksternBehandlingIdRepository: EksternBehandlingIdRepository

    @Test
    internal fun `Skal returnere 403 dersom man ikke har tilgang til brukeren`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("ikkeTilgang"))))
        val behandling = testoppsettService.lagre(behandling(fagsak))

        kall.behandling.apiRespons
            .hent(behandling.id)
            .expectStatus()
            .isForbidden()
    }

    @Test
    internal fun `Skal henlegge behandling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("12345678901"))))
        val behandling = testoppsettService.lagre(behandling(fagsak, type = BehandlingType.FØRSTEGANGSBEHANDLING))
        val henlagtBegrunnelse = "Registert feil"

        kall.behandling.henlegg(behandling.id, HenlagtDto(årsak = HenlagtÅrsak.FEILREGISTRERT, begrunnelse = henlagtBegrunnelse))

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

        val behandlingId = kall.behandling.ekstern(eskternBehandlingId.id)

        val oppdatert = eksternBehandlingIdRepository.findByIdOrThrow(eskternBehandlingId.id)
        assertThat(oppdatert.id).isEqualTo(eskternBehandlingId.id)
        assertThat(oppdatert.behandlingId.id).isEqualTo(behandlingId.id)
    }

    @Test
    internal fun `Skal henlegge FØRSTEGANGSBEHANDLING`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("12345678901"))))
        val behandling = testoppsettService.lagre(behandling(fagsak, type = BehandlingType.FØRSTEGANGSBEHANDLING))
        val henlagtBegrunnelse = "Registert feil"

        kall.behandling.henlegg(behandling.id, HenlagtDto(årsak = HenlagtÅrsak.FEILREGISTRERT, begrunnelse = henlagtBegrunnelse))

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
        val hentetBehandling = kall.behandling.hent(behandling.id)

        assertThat(hentetBehandling.nyeOpplysningerMetadata?.kilde).isEqualTo(NyeOpplysningerKilde.ETTERSENDING)
        assertThat(hentetBehandling.nyeOpplysningerMetadata?.endringer).containsExactly(NyeOpplysningerEndring.MÅLGRUPPE)
        assertThat(hentetBehandling.nyeOpplysningerMetadata?.beskrivelse).isEqualTo("Hello world")
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
            medBrukercontext(roller = listOf(rolleConfig.veilederRolle)) {
                kall.behandling
                    .apiRespons
                    .hent(behandling.id)
                    .expectStatus()
                    .isBadRequest
                    .expectProblemDetail(HttpStatus.BAD_REQUEST, "Behandlingen er ikke påbegynt")
            }
        }

        @Test
        fun `behandlingStatus=UTREDES og veilder skal kunne hente behandlingen hvis statusen er annet enn UTREDES`() {
            testoppsettService.oppdater(behandling.copy(status = BehandlingStatus.UTREDES))
            medBrukercontext(roller = listOf(rolleConfig.veilederRolle)) {
                kall.behandling.hent(behandling.id)
            }
        }

        @Test
        fun `behandlingStatus=OPPRETTET skal kunne opprette grunnlag hvis bruker er saksbehandler`() {
            kall.behandling.hent(behandling.id)
        }
    }
}
