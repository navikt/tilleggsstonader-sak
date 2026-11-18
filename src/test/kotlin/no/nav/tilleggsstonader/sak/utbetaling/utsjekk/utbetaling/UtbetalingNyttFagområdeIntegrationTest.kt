package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaFake
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.verdiEllerFeil
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.utbetaling.fagomrade.FagsakUtbetalingsvalgService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class UtbetalingNyttFagområdeIntegrationTest : IntegrationTest() {
    @Autowired
    lateinit var fagsakUtbetalingsvalgService: FagsakUtbetalingsvalgService

    @Autowired lateinit var fagsakRepository: FagsakRepository

    @Test
    fun `iverksetting for pass av barn skal bruke nytt fagområde`() {
        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.BARNETILSYN,
        ) {
            defaultPassAvBarnTestdata()
        }

        val iverksettingDto = hentIverksettingDtoer(1).single()
        assertThat(iverksettingDto.utbetalinger).hasSize(1)
        assertThat(iverksettingDto.utbetalinger.single().brukFagområdeTillst).isFalse
    }

    @Test
    fun `fagsak med historikk på gammelt fagområde skal fortsette med gammelt fagområde`() {
        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.BARNETILSYN,
                tilSteg = StegType.INNGANGSVILKÅR,
            ) {}

        // Simulerer at allerede er satt at den skal betales på gammelt fagområde, eks gammel behandling
        val fagsak = fagsakRepository.findByIdOrThrow(førstegangsbehandlingContext.fagsakId)
        fagsakRepository.update(fagsak.copy(utbetalPåNyttFagområde = false))

        gjennomførBehandlingsløp(førstegangsbehandlingContext.behandlingId, førstegangsbehandlingContext.ident) {
            defaultPassAvBarnTestdata()
        }

        val utbetalPåNyttFagområde =
            fagsakUtbetalingsvalgService.hentEllerSettUtbetalPåNyttFagområde(
                fagsakId = førstegangsbehandlingContext.fagsakId,
            )
        assertThat(utbetalPåNyttFagområde).isFalse
    }

    private fun hentIverksettingDtoer(forventetAntall: Int): List<IverksettingDto> =
        KafkaFake
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, forventetAntall)
            .map { it.verdiEllerFeil<IverksettingDto>() }
}
