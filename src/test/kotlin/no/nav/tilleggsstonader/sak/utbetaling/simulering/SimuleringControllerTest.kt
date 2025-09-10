package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.kall.simulerForBehandling
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.SimuleringsresultatRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.tilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class SimuleringControllerTest : IntegrationTest() {
    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var simuleringsresultatRepository: SimuleringsresultatRepository

    @Autowired
    private lateinit var vedtakRepository: VedtakRepository

    @Test
    internal fun `Skal returnere 200 OK for simulering av behandling`() {
        val personIdent = "12345678901"
        val fagsak = opprettFagsak(personIdent)
        val behandling = opprettBehandling(fagsak)
        opprettVedtak(behandling.id)

        tilkjentYtelseRepository.insert(tilkjentYtelse(behandlingId = behandling.id))

        val respons = simulerForBehandling(behandling.id)

        assertThat(respons.perioder).hasSize(15)
        val simuleringsresultat = simuleringsresultatRepository.findByIdOrThrow(behandling.id)

        // Verifiser at simuleringsresultatet er lagret
        assertThat(simuleringsresultat.data!!.detaljer.perioder).hasSize(16)
        assertThat(simuleringsresultat.ingenEndringIUtbetaling).isFalse()
    }

    @Test
    internal fun `Skal håndtere 204 No Content for behandling uten endring i utbetalinger, og lagre ned dette`() {
        val personIdent = "identIngenEndring"
        val fagsak = opprettFagsak(personIdent)
        val behandling = opprettBehandling(fagsak)
        opprettVedtak(behandling.id)

        tilkjentYtelseRepository.insert(tilkjentYtelse(behandlingId = behandling.id))

        val response = simulerForBehandling(behandling.id)
        assertThat(response.oppsummering).isNull()
        assertThat(response.ingenEndringIUtbetaling).isTrue()

        val simuleringsresultat = simuleringsresultatRepository.findByIdOrThrow(behandling.id)

        assertThat(simuleringsresultat.data).isNull()
        assertThat(simuleringsresultat.ingenEndringIUtbetaling).isTrue()
    }

    private fun opprettFagsak(personIdent: String) = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(personIdent))))

    private fun opprettBehandling(fagsak: Fagsak) =
        testoppsettService.lagre(
            behandling(
                fagsak,
                type = BehandlingType.REVURDERING,
                resultat = BehandlingResultat.INNVILGET,
                status = BehandlingStatus.UTREDES,
                steg = StegType.SIMULERING,
            ),
        )

    private fun opprettVedtak(behandlingId: BehandlingId) {
        vedtakRepository.insert(
            innvilgetVedtak(behandlingId = behandlingId),
        )
    }
}
