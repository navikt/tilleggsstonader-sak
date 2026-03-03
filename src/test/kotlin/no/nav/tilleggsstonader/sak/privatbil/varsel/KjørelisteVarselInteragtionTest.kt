package no.nav.tilleggsstonader.sak.privatbil.varsel

import io.mockk.every
import no.nav.familie.prosessering.domene.Status
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
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

class KjørelisteVarselInteragtionTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var kjørelisteRepository: KjørelisteRepository

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Test
    fun `innvilge rammevedtak privat bil og sjekk varsel for kjøreliste`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val fom = 1 februar 2026
        val tom = 20 mars 2026

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

        val fom = 1 februar 2026
        val tom = 20 mars 2026

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

        val fom = 1 februar 2026
        val tom = 31 mars 2026

        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        ) {
            defaultDagligReisePrivatBilTsoTestdata(fom, tom)
            sendInnKjøreliste {
                periode = Datoperiode(fom, 1 mars 2026)
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

        val fom = 22 desember 2025
        val tom = 4 januar 2026

        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        ) {
            defaultDagligReisePrivatBilTsoTestdata(fom, tom)
            sendInnKjøreliste {
                periode = Datoperiode(fom, 28 desember 2025)
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
