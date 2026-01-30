package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaTestConfig
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.verdiEllerFeil
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UtbetalingLæremidlerIntegrationTest : CleanDatabaseIntegrationTest() {
    @Test
    fun `iverksetting for læremidler skal bruke gammelt fagområde`() {
        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.LÆREMIDLER,
        ) {
            defaultLæremidlerTestdata()
        }

        val iverksettingDto =
            KafkaTestConfig
                .sendteMeldinger()
                .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 1)
                .single()
                .verdiEllerFeil<IverksettingDto>()

        assertThat(iverksettingDto.utbetalinger).hasSize(1)
        assertThat(iverksettingDto.utbetalinger.single().brukFagområdeTillst).isTrue
    }
}
