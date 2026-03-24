package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaFake
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.sendInnKjøreliste
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteRepository
import no.nav.tilleggsstonader.sak.util.KjørelisteSkjemaUtil
import no.nav.tilleggsstonader.sak.util.KjørelisteSkjemaUtil.kjørelisteSkjema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class KjørelisteKlarTilInnsendingIntegrationTest : IntegrationTest() {
    @Autowired
    lateinit var kjørelisteRepository: KjørelisteRepository

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    val dagensDato = LocalDate.now()
    val fom: LocalDate = dagensDato.minusWeeks(3)
    val tom: LocalDate = dagensDato.plusWeeks(3)

    @Test
    fun `sjekke at kjørelister i nåværende uke og tre uker tilbake i tid er klare for innsending`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)
            }

        // Sjekk at ingenting blir utbetalt
        KafkaFake
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 0)

        // Sjekk at rammevedtaket kan hentes
        val rammevedtak = kall.privatBil.hentRammevedtak(behandlingContext.ident)
        val reiseId = rammevedtak.single().reiseId

        val dagerKjørt =
            listOf(
                KjørelisteSkjemaUtil.KjørtDag(dagensDato.minusWeeks(2)),
                KjørelisteSkjemaUtil.KjørtDag(dagensDato.minusWeeks(1)),
                KjørelisteSkjemaUtil.KjørtDag(dagensDato.minusDays(2)),
            )
        val kjøreliste =
            kjørelisteSkjema(
                reiseId = reiseId,
                periode = Datoperiode(fom, tom),
                dagerKjørt = dagerKjørt,
            )

        // Send inn kjøreliste
        val journalpostId = sendInnKjøreliste(kjøreliste, behandlingContext.ident)

        // Verifisere kjøreliste-journalpost blitt arkivert
        verify(exactly = 1) {
            journalpostClient.ferdigstillJournalpost(
                journalpostId = journalpostId,
                journalførendeEnhet = "9999",
                saksbehandler = "VL",
            )
        }

        assertThat(rammevedtak).hasSize(1)
        assertThat(rammevedtak.flatMap { it.uker.map { uke -> uke.kanSendeInnKjøreliste } })
            .containsExactly(true, true, true, false, false, false, false)
    }
}
