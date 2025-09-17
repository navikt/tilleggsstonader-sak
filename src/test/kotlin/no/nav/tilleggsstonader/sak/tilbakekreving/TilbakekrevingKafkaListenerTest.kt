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
import no.nav.tilleggsstonader.sak.tilbakekreving.TilbakekrevingKafkaListener.Companion.HENDELSESTYPE_FAGSYSTEMINFO_BEHOV
import no.nav.tilleggsstonader.sak.utbetaling.AndelMedVedtaksperioder
import no.nav.tilleggsstonader.sak.utbetaling.AndelTilkjentYtelseTilPeriodeService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import java.time.Instant
import java.util.UUID

class TilbakekrevingKafkaListenerTest {
    val behandlingService = mockk<BehandlingService>()
    val eksternBehandlingIdRepository = mockk<EksternBehandlingIdRepository>()
    val kafkaTemplate = mockk<KafkaTemplate<String, SvarTilbakekrevingKravgrunnlagOppslagRecord>>(relaxed = true)
    val andelTilkjentYtelseTilPeriodeService = mockk<AndelTilkjentYtelseTilPeriodeService>()

    val tilbakekrevingKafkaListener =
        TilbakekrevingKafkaListener(
            behandlingService = behandlingService,
            eksternBehandlingIdRepository = eksternBehandlingIdRepository,
            kafkaTemplate = kafkaTemplate,
            andelTilkjentYtelseTilPeriodeService = andelTilkjentYtelseTilPeriodeService,
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
        val behandling = behandling(forrigeIverksatteBehandlingId = BehandlingId.random())

        val payload =
            TilbakekrevingKravgrunnlagOppslagRecord(
                eksternFagsakId = UUID.randomUUID().toString(),
                kravgrunnlagReferanse = eksternBehandlingId.toString(),
                hendelseOpprettet = Instant.now(),
                hendelsestype = HENDELSESTYPE_FAGSYSTEMINFO_BEHOV,
                versjon = 1,
            )

        every { eksternBehandlingIdRepository.findByIdOrThrow(eksternBehandlingId) } returns
            EksternBehandlingId(eksternBehandlingId, behandling.id)
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { andelTilkjentYtelseTilPeriodeService.mapAndelerTilVedtaksperiodeForBehandling(any()) } returns
            listOf(
                AndelMedVedtaksperioder(
                    andelTilkjentYtelse = andelTilkjentYtelse(),
                    vedtaksperiode = vedtaksperiode(),
                ),
            )

        val consumerRecord = ConsumerRecordUtil.lagConsumerRecord(UUID.randomUUID().toString(), objectMapper.writeValueAsString(payload))
        tilbakekrevingKafkaListener.listen(consumerRecord, ack)

        verify(exactly = 1) { kafkaTemplate.send(any<ProducerRecord<String, SvarTilbakekrevingKravgrunnlagOppslagRecord>>()) }
    }

    @Test
    fun `mottar hendelsestype fagsysteminfo_behov, behandling har ikke forrigeIverksattVedtak, kaster feil`() {
        val eksternBehandlingId = 22L
        val behandling = behandling().copy(forrigeIverksatteBehandlingId = null)

        val payload =
            TilbakekrevingKravgrunnlagOppslagRecord(
                eksternFagsakId = UUID.randomUUID().toString(),
                kravgrunnlagReferanse = eksternBehandlingId.toString(),
                hendelseOpprettet = Instant.now(),
                hendelsestype = HENDELSESTYPE_FAGSYSTEMINFO_BEHOV,
                versjon = 1,
            )

        every { eksternBehandlingIdRepository.findByIdOrThrow(eksternBehandlingId) } returns
            EksternBehandlingId(eksternBehandlingId, behandling.id)
        every { behandlingService.hentBehandling(behandling.id) } returns behandling

        val consumerRecord = ConsumerRecordUtil.lagConsumerRecord(UUID.randomUUID().toString(), objectMapper.writeValueAsString(payload))

        assertThatExceptionOfType(Feil::class.java)
            .isThrownBy {
                tilbakekrevingKafkaListener.listen(consumerRecord, ack)
            }
    }
}
