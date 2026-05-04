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
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaFake
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørAlleTaskMedSenererTriggertid
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførKjørelisteBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteRepository
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import no.nav.tilleggsstonader.sak.util.finnNesteSøndag
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
    fun `skal sende kjørelistevarsel hvis ingen uker er sendt inn`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val fom = dagensDato.minusWeeks(3)
        val tom = dagensDato.plusWeeks(3)

        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        ) {
            defaultDagligReisePrivatBilTsoTestdata(fom, tom)
        }

        kjørAlleTaskMedSenererTriggertid()
        KafkaFake
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.dittnav, 1)

        val tasks = taskService.finnAlleTaskerMedType(SendKjorelistevarselTask.TYPE)
        assertThat(tasks.filter { it.status == Status.FERDIG }).hasSize(1)
        assertThat(tasks.filter { it.status == Status.UBEHANDLET }).hasSize(1)
    }

    @Test
    fun `skal sende kjørelistevarsel hvis et av to rammevedtak mangler kjøreliste`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val fom1 = 1 januar 2026
        val tom1 = 31 januar 2026

        val fom2 = 1 februar 2026
        val tom2 = 28 februar 2026

        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        ) {
            defaultDagligReisePrivatBilTsoTestdata(fom1, tom1) { aktiviteter ->
                aktiviteter.first { it.fom == fom1 }.globalId
            }
            defaultDagligReisePrivatBilTsoTestdata(fom2, tom2) { aktiviteter ->
                aktiviteter.first { it.fom == fom2 }.globalId
            }
            sendInnKjøreliste {
                periode = Datoperiode(fom1, tom1)
                kjørteDager =
                    listOf(
                        KjørtDag(dato = fom1, parkeringsutgift = 50),
                    )
                reiseIdProvider = { it.first().reiseId }
            }
        }

        kjørAlleTaskMedSenererTriggertid()
        KafkaFake
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.dittnav, 1)

        val tasks = taskService.finnAlleTaskerMedType(SendKjorelistevarselTask.TYPE)
        assertThat(tasks.filter { it.status == Status.FERDIG }).hasSize(1)
        assertThat(tasks.filter { it.status == Status.UBEHANDLET }).hasSize(1)
    }

    @Test
    fun `skal ikke sende kjørelistevarsel hvis alle uker er sendt inn`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val fom = 1 januar 2026
        val tom = 31 januar 2026

        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        ) {
            defaultDagligReisePrivatBilTsoTestdata(fom, tom)
            sendInnKjøreliste {
                periode = Datoperiode(fom, tom)
                kjørteDager =
                    listOf(
                        KjørtDag(dato = fom, parkeringsutgift = 50),
                    )
            }
        }

        kjørAlleTaskMedSenererTriggertid()
        KafkaFake
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.dittnav, 0)

        val tasks = taskService.finnAlleTaskerMedType(SendKjorelistevarselTask.TYPE)
        assertThat(tasks.filter { it.status == Status.FERDIG }).hasSize(1)
        assertThat(tasks.filter { it.status == Status.UBEHANDLET }).hasSize(0)
    }

    @Test
    fun `skal ikke sende kjørelistevarsel hvis alt frem til forrige uke er sendt inn`() {
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
                        KjørtDag(dato = fom, parkeringsutgift = 50),
                    )
            }
        }

        kjørAlleTaskMedSenererTriggertid()
        KafkaFake
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.dittnav, 0)

        val tasks = taskService.finnAlleTaskerMedType(SendKjorelistevarselTask.TYPE)
        assertThat(tasks.filter { it.status == Status.FERDIG }).hasSize(1)
        assertThat(tasks.filter { it.status == Status.UBEHANDLET }).hasSize(1)
    }

    @Test
    fun `skal sende kjørelistevarsel hvis det er sendt kjøreliste for uke 52 og ikke uke 1`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val fom = 22 desember 2025
        val tom = 4 januar 2026
        val tomKjoreliste = 28 desember 2025

        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        ) {
            defaultDagligReisePrivatBilTsoTestdata(fom, tom)
            sendInnKjøreliste {
                periode = Datoperiode(fom, tomKjoreliste)
                kjørteDager =
                    listOf(
                        KjørtDag(dato = fom, parkeringsutgift = 50),
                    )
            }
        }

        kjørAlleTaskMedSenererTriggertid()
        KafkaFake
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.dittnav, 1)

        val tasks = taskService.finnAlleTaskerMedType(SendKjorelistevarselTask.TYPE)
        assertThat(tasks.filter { it.status == Status.FERDIG }).hasSize(1)
        assertThat(tasks.filter { it.status == Status.UBEHANDLET }).hasSize(1)
    }

    @Test
    fun `skal kun sende ett varsel etter det er blitt behandlet kjørelistebehandling`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val fom = 2 mars 2026
        val tom = 29 mars 2026

        val behandlingcontext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)
                sendInnKjøreliste {
                    periode = Datoperiode(fom, fom.finnNesteSøndag())
                    kjørteDager =
                        listOf(
                            KjørtDag(dato = fom, parkeringsutgift = 50),
                        )
                }
            }

        val kjørelistebehandling =
            testoppsettService
                .hentBehandlinger(behandlingcontext.fagsakId)
                .single { it.type == BehandlingType.KJØRELISTE }
        gjennomførKjørelisteBehandling(kjørelistebehandling)

        kjørAlleTaskMedSenererTriggertid()
        KafkaFake
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.dittnav, 1)

        val tasks = taskService.finnAlleTaskerMedType(SendKjorelistevarselTask.TYPE)
        // 1 for rammevedtak-behandling. Denne skal ikke ha produsert varsel, og en for kjørelistebehandling som skal ha produsert varsel
        assertThat(tasks.filter { it.status == Status.FERDIG }).hasSize(2)
        assertThat(tasks.filter { it.status == Status.UBEHANDLET }).hasSize(1)
    }
}
