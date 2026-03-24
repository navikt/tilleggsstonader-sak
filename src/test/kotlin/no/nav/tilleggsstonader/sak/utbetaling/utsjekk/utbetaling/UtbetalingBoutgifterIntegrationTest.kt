package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaFake
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.verdiEllerFeil
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UtbetalingBoutgifterIntegrationTest : IntegrationTest() {
    @Test
    fun `iverksetting for boutgifter skal bruke gammelt fagområde`() {
        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.BOUTGIFTER,
        ) {
            defaultBoutgifterTestdata()
        }

        val iverksettingDto =
            KafkaFake
                .sendteMeldinger()
                .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 1)
                .single()
                .verdiEllerFeil<IverksettingDto>()

        assertThat(iverksettingDto.utbetalinger).hasSize(1)
        assertThat(iverksettingDto.utbetalinger.single().brukFagområdeTillst).isTrue
    }
}
