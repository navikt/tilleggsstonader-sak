package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.dto.BehandlingDto
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.tilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnVedtakRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.exchange
import java.util.UUID

internal class SimuleringControllerTest : IntegrationTest() {

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var simuleringsresultatRepository: SimuleringsresultatRepository

    @Autowired
    private lateinit var tilsynBarnVedtakRepository: TilsynBarnVedtakRepository

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(onBehalfOfToken())
    }

    @Test
    internal fun `Skal returnere 200 OK for simulering av behandling`() {
        val personIdent = "12345678901"
        val fagsak = opprettFagsak(personIdent)
        val behandling = opprettBehandling(fagsak)
        opprettVedtak(behandling.id)

        tilkjentYtelseRepository.insert(tilkjentYtelse(behandlingId = behandling.id))

        val respons: ResponseEntity<SimuleringDto> = simulerForBehandling(behandling.id)

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body!!.perioder).hasSize(16)
        val simuleringsresultat = simuleringsresultatRepository.findByIdOrThrow(behandling.id)

        // Verifiser at simuleringsresultatet er lagret
        assertThat(simuleringsresultat.data.detaljer.perioder).hasSize(16)
    }

    private fun opprettFagsak(personIdent: String) =
        testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(personIdent))))

    private fun opprettBehandling(fagsak: Fagsak) = testoppsettService.lagre(
        behandling(
            fagsak,
            type = BehandlingType.REVURDERING,
            resultat = BehandlingResultat.INNVILGET,
            status = BehandlingStatus.UTREDES,
            steg = StegType.SIMULERING,
        ),
    )

    private fun simulerForBehandling(behandlingId: UUID): ResponseEntity<SimuleringDto> {
        return restTemplate.exchange(
            localhost("/api/simulering/$behandlingId"),
            HttpMethod.GET,
            HttpEntity<BehandlingDto>(headers),
        )
    }

    private fun opprettVedtak(behandlingId: UUID) {
        tilsynBarnVedtakRepository.insert(
            innvilgetVedtak(behandlingId = behandlingId),
        )
    }
}
