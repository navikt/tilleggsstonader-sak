package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.oktober
import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaTestConfig
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.sendInnKjøreliste
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteRepository
import no.nav.tilleggsstonader.sak.privatbil.ReisevurderingPrivatBilDto
import no.nav.tilleggsstonader.sak.util.KjørelisteSkjemaUtil
import no.nav.tilleggsstonader.sak.util.KjørelisteSkjemaUtil.kjørelisteSkjema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.client.expectBody
import java.time.LocalDate

class InnvilgePrivatBilIntegrationTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var kjørelisteRepository: KjørelisteRepository

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    val fom: LocalDate = 15 september 2025
    val tom: LocalDate = 14 oktober 2025

    @Test
    fun `innvilge rammevedtak privat bil og henter ut rammevedtak`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val behandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)
            }
        val saksbehandling = testoppsettService.hentSaksbehandling(behandlingId)

        // Sjekk at ingenting blir utbetalt
        KafkaTestConfig
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 0)

        // Sjekk at rammevedtaket kan hentes
        val rammevedtak = kall.privatBil.hentRammevedtak("12345678910")
        val reiseId = rammevedtak.single().reiseId

        assertThat(rammevedtak).hasSize(1)
        assertThat(rammevedtak.single().fom).isEqualTo(fom)
        assertThat(rammevedtak.single().tom).isEqualTo(tom)

        val dagerKjørt =
            listOf(
                KjørelisteSkjemaUtil.KjørtDag(15 september 2025),
                KjørelisteSkjemaUtil.KjørtDag(20 september 2025),
                KjørelisteSkjemaUtil.KjørtDag(23 september 2025),
            )
        val kjøreliste =
            kjørelisteSkjema(
                reiseId = reiseId.toString(),
                periode = Datoperiode(fom, tom),
                dagerKjørt = dagerKjørt,
            )

        // Send inn kjøreliste
        val journalpostId = sendInnKjøreliste(kjøreliste)

        // Verifisere kjøreliste-journalpost blitt arkivert
        verify(exactly = 1) {
            journalpostClient.ferdigstillJournalpost(
                journalpostId = journalpostId,
                journalførendeEnhet = "9999",
                saksbehandler = "VL",
            )
        }

        val behandlingerPåFagsak = behandlingRepository.findByFagsakId(saksbehandling.fagsakId)
        assertThat(behandlingerPåFagsak).hasSize(2)
        // TODO - bør behandlingstype si at det er en kjøreliste?
        // TODO - verifiser at behandlingsstatistikk finnes
        val kjørelisteBehandling = behandlingerPåFagsak.single { it.årsak == BehandlingÅrsak.KJØRELISTE }

        val hentetKjøreliste =
            restTestClient
                .get()
                .uri("/api/kjoreliste/${kjørelisteBehandling.id}")
                .medOnBehalfOfToken()
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<List<ReisevurderingPrivatBilDto>>()
                .returnResult()
                .responseBody

        assertThat(hentetKjøreliste).isNotNull.isNotEmpty
        // Sjekker at alle dager fra kjøreliste kommer i respons
        assertThat(
            hentetKjøreliste!!
                .single()
                .uker
                .flatMap { it.dager }
                .filter { it.dato in dagerKjørt.map { d -> d.dato } },
        ).allMatch {
            it.kjørelisteDag?.harKjørt == true
        }
    }
}
