package no.nav.tilleggsstonader.sak.tilbakekreving

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.libs.log.logger
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

            kafkaTemplate
                .send(
                    ProducerRecord(
                        TILBAKEKREVING_TOPIC,
                        consumerRecord.key(),
                        objectMapper.writeValueAsString(svarTilbakekrevingKravgrunnlagOppslagRecord).also { println(it) },
                    ),
                ).get()
        } else {
            logger.info("fikk hendelsestype $hendelsestype")
        }

        ack.acknowledge()
    }

    private fun mapRevurderinginformsjon(
        saksbehandling: Saksbehandling,
        eksternBehandlingId: String,
    ): TilbakekrevingFagsysteminfoSvarRevurdering {
        val vedtak = vedtakService.hentVedtak(saksbehandling.id) ?: error("Finner ikke vedtak for behandling ${saksbehandling.id}")

        return TilbakekrevingFagsysteminfoSvarRevurdering(
            behandlingId = eksternBehandlingId,
            årsak = mapÅrsak(saksbehandling),
            årsakTilFeilutbetaling = (vedtak.data as Opphør).begrunnelse,
            vedtaksdato = saksbehandling.vedtakstidspunkt!!.toLocalDate(),
        )
    }

    private fun mapÅrsak(saksbehandling: Saksbehandling): TilbakekrevingRevurderingÅrsak =
        when (saksbehandling.årsak) {
            BehandlingÅrsak.KLAGE -> TilbakekrevingRevurderingÅrsak.KLAGE
            BehandlingÅrsak.NYE_OPPLYSNINGER -> TilbakekrevingRevurderingÅrsak.NYE_OPPLYSNINGER
            BehandlingÅrsak.KORRIGERING_UTEN_BREV -> TilbakekrevingRevurderingÅrsak.KORRIGERING
            BehandlingÅrsak.MANUELT_OPPRETTET -> TilbakekrevingRevurderingÅrsak.KORRIGERING
            BehandlingÅrsak.MANUELT_OPPRETTET_UTEN_BREV -> TilbakekrevingRevurderingÅrsak.KORRIGERING
            BehandlingÅrsak.SØKNAD, BehandlingÅrsak.PAPIRSØKNAD, BehandlingÅrsak.SATSENDRING -> TilbakekrevingRevurderingÅrsak.UKJENT
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
