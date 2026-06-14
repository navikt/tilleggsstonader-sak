package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaFake
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.verdiEllerFeil
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.utbetaling.fagomrade.FagsakUtbetalingsvalgService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class UtbetalingBoutgifterIntegrationTest : IntegrationTest() {
    @Autowired
    lateinit var fagsakUtbetalingsvalgService: FagsakUtbetalingsvalgService

    @Test
    fun `iverksetting for boutgifter skal bruke gammelt fagområde når toggle er av`() {
        every { unleashService.isEnabled(Toggle.BRUK_NYTT_FAGOMRADE_FOR_UTBETALING) } returns false
        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.BOUTGIFTER,
        ) {
            defaultBoutgifterTestdata()
        }

        val iverksettingDto = hentIverksettingDtoer(1).single()

        assertThat(iverksettingDto.utbetalinger).hasSize(1)
        assertThat(iverksettingDto.utbetalinger.single().brukFagområdeTillst).isTrue
    }

    @Test
    fun `iverksetting for boutgifter skal bruke nytt fagområde når toggle er på`() {
        every { unleashService.isEnabled(Toggle.BRUK_NYTT_FAGOMRADE_FOR_UTBETALING) } returns true
        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.BOUTGIFTER,
        ) {
            defaultBoutgifterTestdata()
        }

        val iverksettingDto = hentIverksettingDtoer(1).single()
        assertThat(iverksettingDto.utbetalinger).hasSize(1)
        assertThat(iverksettingDto.utbetalinger.single().brukFagområdeTillst).isFalse
    }

    @Test
    fun `fagsak med historikk på gammelt fagområde skal fortsette med gammelt fagområde`() {
        every { unleashService.isEnabled(Toggle.BRUK_NYTT_FAGOMRADE_FOR_UTBETALING) } returns false
        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.BOUTGIFTER,
            ) {
                defaultBoutgifterTestdata()
            }

        every { unleashService.isEnabled(Toggle.BRUK_NYTT_FAGOMRADE_FOR_UTBETALING) } returns true
        val utbetalPåNyttFagområde =
            fagsakUtbetalingsvalgService.hentEllerSettUtbetalPåNyttFagområde(
                fagsakId = førstegangsbehandlingContext.fagsakId,
                stønadstype = Stønadstype.BOUTGIFTER,
            )
        assertThat(utbetalPåNyttFagområde).isFalse
    }

    private fun hentIverksettingDtoer(forventetAntall: Int): List<IverksettingDto> =
        KafkaFake
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, forventetAntall)
            .map { it.verdiEllerFeil<IverksettingDto>() }
}
