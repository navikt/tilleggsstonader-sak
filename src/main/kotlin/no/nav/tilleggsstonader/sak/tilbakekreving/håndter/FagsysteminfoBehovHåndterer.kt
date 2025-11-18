package no.nav.tilleggsstonader.sak.tilbakekreving.håndter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingIdRepository
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.tilbakekreving.TILBAKEKREVING_TOPIC
import no.nav.tilleggsstonader.sak.tilbakekreving.hendelse.TILBAKEKREVING_TYPE_FAGSYSTEMINFO_BEHOV
import no.nav.tilleggsstonader.sak.tilbakekreving.hendelse.TilbakekrevingFagsysteminfoBehov
import no.nav.tilleggsstonader.sak.tilbakekreving.hendelse.TilbakekrevingFagsysteminfoSvar
import no.nav.tilleggsstonader.sak.tilbakekreving.hendelse.TilbakekrevingFagsysteminfoSvarRevurdering
import no.nav.tilleggsstonader.sak.tilbakekreving.hendelse.TilbakekrevingMottaker
import no.nav.tilleggsstonader.sak.tilbakekreving.hendelse.TilbakekrevingPeriode
import no.nav.tilleggsstonader.sak.tilbakekreving.hendelse.TilbakekrevingRevurderingÅrsak
import no.nav.tilleggsstonader.sak.tilbakekreving.hendelse.UtvidetPeriode
import no.nav.tilleggsstonader.sak.utbetaling.AndelTilkjentYtelseTilPeriodeService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.Opphør
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class FagsysteminfoBehovHåndterer(
    private val fagsakService: FagsakService,
    private val eksternBehandlingIdRepository: EksternBehandlingIdRepository,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val andelTilkjentYtelseTilPeriodeService: AndelTilkjentYtelseTilPeriodeService,
    private val kafkaTemplate: KafkaTemplate<String, String>,
) : TilbakekrevingHendelseHåndterer {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun håndtererHendelsetype(): String = TILBAKEKREVING_TYPE_FAGSYSTEMINFO_BEHOV

    override fun håndter(
        hendelseKey: String,
        payload: JsonNode,
    ) {
        val fagsystemBehovMelding = objectMapper.treeToValue<TilbakekrevingFagsysteminfoBehov>(payload)

        // Team tilbake bruker også kafka-topic til intern testing i dev, filtrerer vekk meldinger ikke ment for oss
        if (gjelderTestsak(fagsystemBehovMelding)) {
            logger.debug(
                "Mottatt hendelse $TILBAKEKREVING_TYPE_FAGSYSTEMINFO_BEHOV med ugyldig eksternFagsakId=${fagsystemBehovMelding.eksternFagsakId}, ignorerer melding",
            )
        } else if (fagsakService.hentFagsakPåEksternIdHvisEksisterer(fagsystemBehovMelding.eksternFagsakId.toLong()) == null) {
            logger.warn("Finner ikke faksak med eksternId ${fagsystemBehovMelding.eksternFagsakId}")
        } else {
            behandleFagsystemInfoBehov(hendelseKey, fagsystemBehovMelding)
        }
    }

    private fun behandleFagsystemInfoBehov(
        kafkaKey: String,
        fagsystemBehovMelding: TilbakekrevingFagsysteminfoBehov,
    ) {
        val referanse = fagsystemBehovMelding.kravgrunnlagReferanse ?: error("Ikke mottatt referanse fra tilbakekreving")

        logger.info(
            "Mottatt hendelse ${fagsystemBehovMelding.hendelsestype} fra tilbakekreving for behandlingId=$referanse og fagsak=${fagsystemBehovMelding.eksternFagsakId}",
        )

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
                hendelseOpprettet = LocalDateTime.now(),
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
