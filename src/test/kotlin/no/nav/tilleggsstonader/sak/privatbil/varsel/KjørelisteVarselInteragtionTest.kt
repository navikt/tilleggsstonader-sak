package no.nav.tilleggsstonader.sak.privatbil.varsel

import io.mockk.every
import no.nav.familie.prosessering.domene.Status
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaTestConfig
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørAlleTaskMedSenererTriggertid
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class KjørelisteVarselInteragtionTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var kjørelisteRepository: KjørelisteRepository

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    val dagensDato = LocalDate.now()

    @Test
    fun `innvilge rammevedtak privat bil og sjekk varsel for kjøreliste`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val fom = dagensDato.minusDays(3)
        val tom = dagensDato.plusWeeks(3)

        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        ) {
            defaultDagligReisePrivatBilTsoTestdata(fom, tom)
        }

        kjørAlleTaskMedSenererTriggertid()
        KafkaTestConfig
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.dittnav, 1)

        val tasks = taskService.finnAlleTaskerMedType(SendKjorelisteTask.TYPE).filter { it.status == Status.UBEHANDLET }
        assertThat(tasks).hasSize(1)
    }

    @Test
    fun `innvilge rammevedtak og sende inn kjørelister for alle dager og at det ikke blir sendt varsel`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val fom = dagensDato.plusWeeks(1)
        val tom = dagensDato.plusWeeks(6)

        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        ) {
            defaultDagligReisePrivatBilTsoTestdata(fom, tom)
            sendInnKjøreliste {
                periode = Datoperiode(fom, tom)
                kjørteDager =
                    listOf(
                        fom to 50,
                    )
            }
        }

        kjørAlleTaskMedSenererTriggertid()
        KafkaTestConfig
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.dittnav, 0)

        val tasks =
            taskService.finnAlleTaskerMedType(SendKjorelisteTask.TYPE).filter { it.status == Status.KLAR_TIL_PLUKK }
        assertThat(tasks).hasSize(0)
    }

    @Test
    fun `innvilge rammevedtak og sende inn alle mulige kjørelister hittil og sjekker at varsel blir sendt `() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val fom = dagensDato.minusWeeks(4)
        val tom = dagensDato.plusWeeks(4)
        val tomKjoreliste = dagensDato.minusWeeks(1)

        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        ) {
            defaultDagligReisePrivatBilTsoTestdata(fom, tom)
            sendInnKjøreliste {
                periode = Datoperiode(fom, tomKjoreliste)
                kjørteDager =
                    listOf(
                        fom to 50,
                    )
            }
        }

        kjørAlleTaskMedSenererTriggertid()
        KafkaTestConfig
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.dittnav, 0)

        val tasks = taskService.finnAlleTaskerMedType(SendKjorelisteTask.TYPE)
        assertThat(tasks.filter { it.status == Status.KLAR_TIL_PLUKK }).hasSize(0)
        assertThat(tasks.filter { it.status == Status.FERDIG }).hasSize(1)
    }

    @Test
    fun `innvilge rammevedtak som strekker seg over et år`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        // Periode fra siste uke i fjor til første uke i år
        val iÅr = dagensDato.year
        val iFjor = iÅr - 1
        val fom = LocalDate.of(iFjor, 12, 27)
        val tom = LocalDate.of(iÅr, 1, 9)
        val tomKjoreliste = fom.plusDays(6)

        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        ) {
            defaultDagligReisePrivatBilTsoTestdata(fom, tom)
            sendInnKjøreliste {
                periode = Datoperiode(fom, tomKjoreliste)
                kjørteDager =
                    listOf(
                        fom to 50,
                    )
            }
        }

        kjørAlleTaskMedSenererTriggertid()
        KafkaTestConfig
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.dittnav, 1)

        val tasks = taskService.finnAlleTaskerMedType(SendKjorelisteTask.TYPE)
        assertThat(tasks.filter { it.status == Status.KLAR_TIL_PLUKK }).hasSize(0)
        assertThat(tasks.filter { it.status == Status.FERDIG }).hasSize(1)
    }
}
