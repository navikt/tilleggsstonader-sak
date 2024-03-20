package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.dto.BehandlingDto
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.Simuleringsoppsummering
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.tilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
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
import org.springframework.web.client.exchange
import java.util.UUID

internal class SimuleringControllerTest : IntegrationTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var simuleringsresultatRepository: SimuleringsresultatRepository

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(onBehalfOfToken())
    }

    @Test
    internal fun `Skal returnere 200 OK for simulering av behandling`() {
        val personIdent = "12345678901"
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(personIdent))))
        val behandling = testoppsettService.lagre(behandling(fagsak))
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandlingId = behandling.id))

        val respons: ResponseEntity<Simuleringsoppsummering> = simulerForBehandling(behandling.id)

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body!!.perioder).hasSize(8)
        val simuleringsresultat = simuleringsresultatRepository.findByIdOrThrow(behandling.id)

        // Verifiser at simuleringsresultatet er lagret
        assertThat(simuleringsresultat.data.detaljer.simuleringMottaker).hasSize(1)
        assertThat(simuleringsresultat.data.detaljer.simuleringMottaker.first().simulertPostering)
            .hasSizeGreaterThan(1)
    }

    private fun simulerForBehandling(behandlingId: UUID): ResponseEntity<Simuleringsoppsummering> {
        return restTemplate.exchange(
            localhost("/api/simulering/$behandlingId"),
            HttpMethod.GET,
            HttpEntity<BehandlingDto>(headers),
        )
    }
}
