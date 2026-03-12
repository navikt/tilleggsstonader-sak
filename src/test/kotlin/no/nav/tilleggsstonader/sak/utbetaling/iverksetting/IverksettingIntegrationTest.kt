package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaFake
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import org.junit.jupiter.api.Test
import java.time.LocalDate

class IverksettingIntegrationTest : CleanDatabaseIntegrationTest() {
    @Test
    fun `skal ikke sende noe på kafka hvis vi bare har andeler fram i tid`() {
        val omTrettiDager = LocalDate.now().plusDays(30)
        val omSekstiDager = LocalDate.now().plusDays(60)
        val omNittiDager = LocalDate.now().plusDays(90)

        val førstegangsbehandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(Stønadstype.DAGLIG_REISE_TSO) {
                defaultDagligReiseTsoTestdata(fom = omTrettiDager, tom = omSekstiDager)
            }

        KafkaFake
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 0)

        opprettRevurderingOgGjennomførBehandlingsløp(førstegangsbehandlingId) {
            defaultDagligReiseTsoTestdata(fom = omSekstiDager.plusDays(1), tom = omNittiDager)
        }

        KafkaFake
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 0)
    }
}
