package no.nav.tilleggsstonader.sak.tilbakekreving

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.libs.log.logger
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingIdRepository
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.AndelTilkjentYtelseTilPeriodeService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class TilbakekrevingKafkaListener(
    private val behandlingService: BehandlingService,
    private val eksternBehandlingIdRepository: EksternBehandlingIdRepository,
    private val kafkaTemplate: KafkaTemplate<String, SvarTilbakekrevingKravgrunnlagOppslagRecord>,
    private val andelTilkjentYtelseTilPeriodeService: AndelTilkjentYtelseTilPeriodeService,
) {
    companion object {
        const val TILBAKEKREVING_TOPIC = "privat-tilbakekreving-andeler"
        const val HENDELSESTYPE_FAGSYSTEMINFO_BEHOV = "fagsysteminfo_behov"
    }

    @KafkaListener(
        groupId = "tilleggsstonader-sak",
        topics = [TILBAKEKREVING_TOPIC],
        containerFactory = "tilbakekrevingKravgrunnlagOppslagListenerContainerFactory",
    )
    fun listen(
        consumerRecord: ConsumerRecord<String, String>,
        ack: Acknowledgment,
    ) {
        val payload = objectMapper.readTree(consumerRecord.value())
        val hendelsestype = payload.get("hendelsestype")?.asText() ?: error("Mangler felt 'hendelsestype' i melding fra tilbakekreving")

        if (hendelsestype == HENDELSESTYPE_FAGSYSTEMINFO_BEHOV) {
            val fagsystemBehovMelding = objectMapper.treeToValue<TilbakekrevingKravgrunnlagOppslagRecord>(payload)
            val referanse = fagsystemBehovMelding.kravgrunnlagReferanse ?: error("Ikke mottatt referanse fra tilbakekreving")
            val behandlingId =
                eksternBehandlingIdRepository
                    .findByIdOrThrow(referanse.toLong())
                    .behandlingId

            val behandling = behandlingService.hentBehandling(behandlingId)

            feilHvis(behandling.forrigeIverksatteBehandlingId == null) {
                "Behandling med id=$behandlingId har ingen forrige iverksatte behandling"
            }

            val svarTilbakekrevingKravgrunnlagOppslagRecord =
                SvarTilbakekrevingKravgrunnlagOppslagRecord(
                    eksternFagsakId = fagsystemBehovMelding.eksternFagsakId,
                    referanse = referanse,
                    andeler = lagAndelerForBehandling(behandling),
                    revurderingÅrsak = behandling.årsak.tilRevurderingÅrsak(),
                )

            kafkaTemplate
                .send(
                    ProducerRecord(
                        TILBAKEKREVING_TOPIC,
                        consumerRecord.key(),
                        svarTilbakekrevingKravgrunnlagOppslagRecord,
                    ),
                ).get()
        } else {
            logger.info("fikk hendelsestype $hendelsestype")
        }

        ack.acknowledge()
    }

    private fun lagAndelerForBehandling(behandling: Behandling): AndelerUtbetalingDto =
        AndelerUtbetalingDto(
            andelerForBehandling =
                mapAndelerForBehandling(behandling.id)
                    .sortedBy { it.utbetalingsperiode.fom },
            andelerForForrigeBehandling =
                mapAndelerForBehandling(behandling.forrigeIverksatteBehandlingId!!)
                    .sortedBy { it.utbetalingsperiode.fom },
        )

    private fun mapAndelerForBehandling(behandlingId: BehandlingId): List<AndelUtbetalingMedVedtaksperiodeDto> =
        andelTilkjentYtelseTilPeriodeService.mapAndelerTilVedtaksperiodeForBehandling(behandlingId).map {
            AndelUtbetalingMedVedtaksperiodeDto(
                utbetalingsperiode =
                    PeriodeDto(
                        fom = it.andelTilkjentYtelse.fom,
                        tom = it.andelTilkjentYtelse.tom,
                    ),
                vedtaksperiode = it.vedtaksperiode?.let { v -> PeriodeDto(v.fom, v.tom) },
                beløp = it.andelTilkjentYtelse.beløp,
                typeAndel = it.andelTilkjentYtelse.type,
            )
        }
}

private fun BehandlingÅrsak.tilRevurderingÅrsak(): SvarTilbakekrevingKravgrunnlagOppslagRevurderingÅrsak =
    when (this) {
        BehandlingÅrsak.KLAGE -> SvarTilbakekrevingKravgrunnlagOppslagRevurderingÅrsak.KLAGE
        BehandlingÅrsak.NYE_OPPLYSNINGER -> SvarTilbakekrevingKravgrunnlagOppslagRevurderingÅrsak.NYE_OPPLYSNINGER
        BehandlingÅrsak.KORRIGERING_UTEN_BREV -> SvarTilbakekrevingKravgrunnlagOppslagRevurderingÅrsak.KORRIGERING
        BehandlingÅrsak.SØKNAD,
        BehandlingÅrsak.PAPIRSØKNAD,
        BehandlingÅrsak.SATSENDRING,
        BehandlingÅrsak.MANUELT_OPPRETTET,
        BehandlingÅrsak.MANUELT_OPPRETTET_UTEN_BREV,
        -> SvarTilbakekrevingKravgrunnlagOppslagRevurderingÅrsak.UKJENT
    }

data class SvarTilbakekrevingKravgrunnlagOppslagRecord(
    val eksternFagsakId: String,
    val referanse: String?,
    val andeler: AndelerUtbetalingDto,
    val revurderingÅrsak: SvarTilbakekrevingKravgrunnlagOppslagRevurderingÅrsak,
)

enum class SvarTilbakekrevingKravgrunnlagOppslagRevurderingÅrsak {
    NYE_OPPLYSNINGER,
    KORRIGERING,
    KLAGE,
    UKJENT,
}
