package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaFake
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.sendInnKjøreliste
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteRepository
import no.nav.tilleggsstonader.sak.privatbil.ReisevurderingPrivatBilDto
import no.nav.tilleggsstonader.sak.util.KjørelisteSkjemaUtil
import no.nav.tilleggsstonader.sak.util.KjørelisteSkjemaUtil.kjørelisteSkjema
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaPrivatBil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.client.expectBody
import java.math.BigDecimal
import java.time.LocalDate

class InnvilgePrivatBilIntegrationTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var kjørelisteRepository: KjørelisteRepository

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    val fom: LocalDate = 1 februar 2026
    val tom: LocalDate = 20 mars 2026

    @Test
    fun `innvilge rammevedtak privat bil og henter ut rammevedtak`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)
            }
        val saksbehandling = testoppsettService.hentSaksbehandling(behandlingContext.behandlingId)

        // Sjekk at ingenting blir utbetalt
        KafkaFake
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
                KjørelisteSkjemaUtil.KjørtDag(1 februar 2026),
                KjørelisteSkjemaUtil.KjørtDag(9 februar 2026),
                KjørelisteSkjemaUtil.KjørtDag(22 februar 2026),
            )
        val kjøreliste =
            kjørelisteSkjema(
                reiseId = reiseId,
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

    @Test
    fun `skal lagre og hente fakta med to perioder for privat bil`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)
            }
        val saksbehandling = testoppsettService.hentSaksbehandling(behandlingContext.behandlingId)
        val vilkårListe = kall.vilkårDagligReise.hentVilkår(saksbehandling.id)
        val vilkårPrivatBil = vilkårListe.single { it.fakta.type == TypeDagligReise.PRIVAT_BIL }

        val fakta =
            vilkårPrivatBil.fakta.mapTilFakta(vilkårPrivatBil.reiseId, vilkårPrivatBil.adresse) as FaktaPrivatBil

        assertThat(fakta.reiseId).isNotNull
        assertThat(fakta.reiseavstandEnVei).isEqualTo(BigDecimal(10))
        assertThat(fakta.faktaDelperioder).hasSize(1)
        assertThat(fakta.faktaDelperioder[0].fom).isEqualTo(fom)
        assertThat(fakta.faktaDelperioder[0].tom).isEqualTo(tom)
        assertThat(fakta.faktaDelperioder[0].reisedagerPerUke).isEqualTo(5)
        assertThat(fakta.faktaDelperioder[0].bompengerPerDag).isNull()
        assertThat(fakta.faktaDelperioder[0].fergekostnadPerDag).isNull()
    }
}
