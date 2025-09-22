package no.nav.tilleggsstonader.sak.tilbakekreving

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingIdRepository
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.AndelTilkjentYtelseTilPeriodeService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.Opphør
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate

@Component
@Profile("!local & !integrasjonstest")
class TilbakekrevingKafkaListener(
    private val behandlingService: BehandlingService,
    private val eksternBehandlingIdRepository: EksternBehandlingIdRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val andelTilkjentYtelseTilPeriodeService: AndelTilkjentYtelseTilPeriodeService,
    private val vedtakService: VedtakService,
) {
    companion object {
        const val TILBAKEKREVING_TOPIC = "tilbake.privat-tilbakekreving-tilleggsstonad"
        const val HENDELSESTYPE_FAGSYSTEMINFO_BEHOV = "fagsysteminfo_behov"

        private val logger = LoggerFactory.getLogger(TilbakekrevingKafkaListener::class.java)
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
            val fagsystemBehovMelding = objectMapper.treeToValue<TilbakekrevingFagsysteminfoBehov>(payload)

            // Team tilbake bruker også kafka-topic til intern testing i dev, filtrerer vekk meldinger ikke ment for oss
            if (fagsystemBehovMelding.eksternFagsakId.all { it.isDigit() }) {
                behandleFagsystemInfoBehov(consumerRecord.key(), fagsystemBehovMelding)
            } else {
                logger.debug(
                    "Mottatt hendelse $HENDELSESTYPE_FAGSYSTEMINFO_BEHOV med ugyldig eksternFagsakId=${fagsystemBehovMelding.eksternFagsakId}, ignorerer melding",
                )
            }
        } else {
            // Vi lytter og produserer til samme topic, dvs vi leser inne våre egne meldinger
            logger.info("Ignorerer tilbakekreving-hendelse av type $hendelsestype")
        }

        ack.acknowledge()
    }

    private fun behandleFagsystemInfoBehov(
        kafkaKey: String,
        fagsystemBehovMelding: TilbakekrevingFagsysteminfoBehov,
    ) {
        val referanse = fagsystemBehovMelding.kravgrunnlagReferanse ?: error("Ikke mottatt referanse fra tilbakekreving")
        val behandlingId =
            eksternBehandlingIdRepository
                .findByIdOrThrow(referanse.toLong())
                .behandlingId

        val behandling = behandlingService.hentSaksbehandling(behandlingId)

        feilHvis(behandling.forrigeIverksatteBehandlingId == null) {
            "Behandling med id=$behandlingId har ingen forrige iverksatte behandling"
        }

        val svarTilbakekrevingKravgrunnlagOppslagRecord =
            TilbakekrevingFagsysteminfoSvar(
                eksternFagsakId = fagsystemBehovMelding.eksternFagsakId,
                hendelseOpprettet = Instant.now(),
                mottaker = TilbakekrevingMottaker(ident = behandling.ident),
                revurdering = mapRevurderinginformsjon(saksbehandling = behandling, eksternBehandlingId = referanse),
                utvidPerioder = mapUtvidedePerioder(behandling.forrigeIverksatteBehandlingId),
            )

        // Sender med samme key på kafka, slik at tilbake får meldinger i rekkefølge
        kafkaTemplate
            .send(
                ProducerRecord(
                    TILBAKEKREVING_TOPIC,
                    kafkaKey,
                    objectMapper.writeValueAsString(svarTilbakekrevingKravgrunnlagOppslagRecord),
                ),
            ).get()
    }

    private fun mapRevurderinginformsjon(
        saksbehandling: Saksbehandling,
        eksternBehandlingId: String,
    ): TilbakekrevingFagsysteminfoSvarRevurdering {
        val vedtak = vedtakService.hentVedtak(saksbehandling.id) ?: error("Finner ikke vedtak for behandling ${saksbehandling.id}")

        return TilbakekrevingFagsysteminfoSvarRevurdering(
            behandlingId = eksternBehandlingId,
            årsak = mapÅrsak(saksbehandling),
            årsakTilFeilutbetaling = if (vedtak.data is Opphør) vedtak.data.begrunnelse else null,
            vedtaksdato = saksbehandling.vedtakstidspunkt!!.toLocalDate(),
        )
    }

    private fun mapÅrsak(saksbehandling: Saksbehandling): TilbakekrevingRevurderingÅrsak =
        when (saksbehandling.årsak) {
            BehandlingÅrsak.KLAGE -> TilbakekrevingRevurderingÅrsak.KLAGE
            BehandlingÅrsak.NYE_OPPLYSNINGER -> TilbakekrevingRevurderingÅrsak.NYE_OPPLYSNINGER
            BehandlingÅrsak.KORRIGERING_UTEN_BREV,
            BehandlingÅrsak.MANUELT_OPPRETTET,
            BehandlingÅrsak.MANUELT_OPPRETTET_UTEN_BREV,
            -> TilbakekrevingRevurderingÅrsak.KORRIGERING
            BehandlingÅrsak.SØKNAD,
            BehandlingÅrsak.PAPIRSØKNAD,
            BehandlingÅrsak.SATSENDRING,
            -> TilbakekrevingRevurderingÅrsak.UKJENT
        }

    private fun mapUtvidedePerioder(behandlingId: BehandlingId): List<UtvidetPeriode> =
        andelTilkjentYtelseTilPeriodeService
            .mapAndelerTilVedtaksperiodeForBehandling(behandlingId)
            .map { andelMedVedtaksperioder ->
                UtvidetPeriode(
                    kravgrunnlagPeriode = andelMedVedtaksperioder.andelTilkjentYtelse.let(::mapTilbakekrevingPeriode),
                    vedtaksperiode = andelMedVedtaksperioder.vedtaksperiode?.let(::mapTilbakekrevingPeriode),
                )
            }

    private fun mapTilbakekrevingPeriode(periode: Periode<LocalDate>) = TilbakekrevingPeriode(fom = periode.fom, tom = periode.tom)
}
