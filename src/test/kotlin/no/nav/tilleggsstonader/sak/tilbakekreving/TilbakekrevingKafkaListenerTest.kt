package no.nav.tilleggsstonader.sak.tilbakekreving

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingId
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingIdRepository
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.hendelser.ConsumerRecordUtil
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.tilbakekreving.hendelse.TilbakekrevingFagsysteminfoBehov
import no.nav.tilleggsstonader.sak.tilbakekreving.håndter.FagsysteminfoBehovHåndterer
import no.nav.tilleggsstonader.sak.utbetaling.AndelTilkjentYtelseTilPeriodeService
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

class TilbakekrevingKafkaListenerTest {
    val behandlingService = mockk<BehandlingService>()
    val eksternBehandlingIdRepository = mockk<EksternBehandlingIdRepository>()
    val kafkaTemplate = mockk<KafkaTemplate<String, String>>(relaxed = true)
    val andelTilkjentYtelseTilPeriodeService = mockk<AndelTilkjentYtelseTilPeriodeService>()
    val vedtakService = mockk<VedtakService>()
    val fagsakService = mockk<FagsakService>()
    val oppgaveService = mockk<OppgaveService>()

    val håndterer =
        FagsysteminfoBehovHåndterer(
            fagsakService = fagsakService,
            eksternBehandlingIdRepository = eksternBehandlingIdRepository,
            behandlingService = behandlingService,
            vedtakService = vedtakService,
            andelTilkjentYtelseTilPeriodeService = andelTilkjentYtelseTilPeriodeService,
            kafkaTemplate = kafkaTemplate,
            oppgaveService = oppgaveService,
        )

    val tilbakekrevingHendelseDelegate = TilbakekrevingHendelseDelegate(listOf(håndterer))

    val tilbakekrevingKafkaListener =
        TilbakekrevingKafkaListener(
            tilbakekrevingHendelseDelegate = tilbakekrevingHendelseDelegate,
        )

    val ack = mockk<Acknowledgment>()

    @BeforeEach
    fun setup() {
        justRun { ack.acknowledge() }
    }

    @Test
    fun `mottar hendelsestype fagsysteminfo_behov, behandling har ikke forrigeIverksattVedtak, kaster feil`() {
        val eksternBehandlingId = 22L
        val behandling = saksbehandling(forrigeIverksatteBehandlingId = null)

        val payload =
            TilbakekrevingFagsysteminfoBehov(
                eksternFagsakId = Random.nextLong(10, 10000).toString(),
                kravgrunnlagReferanse = eksternBehandlingId.toString(),
                hendelseOpprettet = LocalDateTime.now(),
                versjon = 1,
            )

        every { fagsakService.hentFagsakPåEksternIdHvisEksisterer(payload.eksternFagsakId.toLong()) } returns fagsak()
        every { eksternBehandlingIdRepository.findByIdOrThrow(eksternBehandlingId) } returns
            EksternBehandlingId(eksternBehandlingId, behandling.id)
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling

        val consumerRecord = ConsumerRecordUtil.lagConsumerRecord(UUID.randomUUID().toString(), jsonMapper.writeValueAsString(payload))

        assertThatExceptionOfType(Feil::class.java)
            .isThrownBy {
                tilbakekrevingKafkaListener.listen(consumerRecord, ack)
            }
    }
}
