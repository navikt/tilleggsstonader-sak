package no.nav.tilleggsstonader.sak.tilbakekreving

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingId
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingIdRepository
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.hendelser.ConsumerRecordUtil
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.utbetaling.AndelMedVedtaksperioder
import no.nav.tilleggsstonader.sak.utbetaling.AndelTilkjentYtelseTilPeriodeService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.opphør
import org.apache.kafka.clients.producer.ProducerRecord
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

    val tilbakekrevingKafkaListener =
        TilbakekrevingKafkaListener(
            behandlingService = behandlingService,
            eksternBehandlingIdRepository = eksternBehandlingIdRepository,
            kafkaTemplate = kafkaTemplate,
            andelTilkjentYtelseTilPeriodeService = andelTilkjentYtelseTilPeriodeService,
            vedtakService = vedtakService,
        )

    val ack = mockk<Acknowledgment>()

    @BeforeEach
    fun setup() {
        justRun { ack.acknowledge() }
    }

    @Test
    fun `mottar ukjent hendelsestype, gjør ingenting`() {
        val payload = mapOf("hendelsestype" to "ukjent_type")
        val consumerRecord = ConsumerRecordUtil.lagConsumerRecord(UUID.randomUUID().toString(), objectMapper.writeValueAsString(payload))
        tilbakekrevingKafkaListener.listen(consumerRecord, ack)

        verify(exactly = 0) { kafkaTemplate.send(any(), any(), any()) }
        verify(exactly = 1) { ack.acknowledge() }
    }

    @Test
    fun `mottar hendelsestype fagsysteminfo_behov, behandling har vedtak, publiserer andeler på topic`() {
        val eksternBehandlingId = 11L
        val behandling = saksbehandling(forrigeIverksatteBehandlingId = BehandlingId.random())

        val payload =
            TilbakekrevingFagsysteminfoBehov(
                eksternFagsakId = Random.nextLong(10, 10000).toString(),
                kravgrunnlagReferanse = eksternBehandlingId.toString(),
                hendelseOpprettet = LocalDateTime.now(),
                versjon = 1,
            )

        every { eksternBehandlingIdRepository.findByIdOrThrow(eksternBehandlingId) } returns
            EksternBehandlingId(eksternBehandlingId, behandling.id)
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling
        every { vedtakService.hentVedtak(behandling.id) } returns opphør()
        every { andelTilkjentYtelseTilPeriodeService.mapAndelerTilVedtaksperiodeForBehandling(any()) } returns
            listOf(
                AndelMedVedtaksperioder(
                    andelTilkjentYtelse = andelTilkjentYtelse(),
                    vedtaksperiode = vedtaksperiode(),
                ),
            )

        val consumerRecord = ConsumerRecordUtil.lagConsumerRecord(UUID.randomUUID().toString(), objectMapper.writeValueAsString(payload))
        tilbakekrevingKafkaListener.listen(consumerRecord, ack)

        verify(exactly = 1) { kafkaTemplate.send(any<ProducerRecord<String, String>>()) }
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

        every { eksternBehandlingIdRepository.findByIdOrThrow(eksternBehandlingId) } returns
            EksternBehandlingId(eksternBehandlingId, behandling.id)
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling

        val consumerRecord = ConsumerRecordUtil.lagConsumerRecord(UUID.randomUUID().toString(), objectMapper.writeValueAsString(payload))

        assertThatExceptionOfType(Feil::class.java)
            .isThrownBy {
                tilbakekrevingKafkaListener.listen(consumerRecord, ack)
            }
    }
}
