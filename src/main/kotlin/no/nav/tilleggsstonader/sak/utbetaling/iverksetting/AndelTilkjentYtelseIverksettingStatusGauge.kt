package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import no.nav.tilleggsstonader.sak.metrics.MetricUtil
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled
import java.time.Duration
import java.time.LocalDateTime

/**
 * Multigauge som holder oversikt over antall iverksettinger hvor:
 * - Vi har mottatt feilet-status fra helved/utsjekk
 * - Vi har ikke mottatt status etter iverksetting
 */
@Configuration
class AndelTilkjentYtelseIverksettingStatusGauge(
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) {
    private val logger = LoggerFactory.getLogger(AndelTilkjentYtelseIverksettingStatusGauge::class.java)

    private val andelerSomHarIverksettingMedUgyldigStatusMultiGauge =
        MultiGauge
            .builder(
                "andel_tilkjent_ytelse_iverksetting_status",
            ).register(Metrics.globalRegistry)

    @Scheduled(initialDelay = MetricUtil.FREKVENS_30_SEC, fixedDelay = MetricUtil.FREKVENS_10_MIN)
    fun antallFeiledeIverksettinger() {
        val sendteOgFeiledeAndeler =
            andelTilkjentYtelseRepository.findAllByStatusIverksettingIn(
                listOf(
                    StatusIverksetting.SENDT,
                    StatusIverksetting.FEILET,
                ),
            )

        val feiledeIverksettinger =
            sendteOgFeiledeAndeler
                .filter { it.statusIverksetting == StatusIverksetting.FEILET }
                .mapNotNull { it.iverksetting?.iverksettingId }
                .distinct()

        if (feiledeIverksettinger.isNotEmpty()) {
            logger.warn("Feilede iverksettinger: {}", feiledeIverksettinger)
        }

        val iverksettingerSendtForEnTimeSiden =
            sendteOgFeiledeAndeler
                .filter { it.statusIverksetting == StatusIverksetting.SENDT }
                .mapNotNull { it.iverksetting }
                .filter { it.iverksettingTidspunkt.isBefore(LocalDateTime.now().minus(Duration.ofHours(1))) }
                .map { it.iverksettingId }
                .distinct()

        if (iverksettingerSendtForEnTimeSiden.isNotEmpty()) {
            logger.warn(
                "Iverksettinger sendt for over en time siden uten oppdatert status: {}",
                iverksettingerSendtForEnTimeSiden,
            )
        }

        andelerSomHarIverksettingMedUgyldigStatusMultiGauge.register(
            listOf(
                MultiGauge.Row.of(Tags.of("status", "FEILET"), feiledeIverksettinger.size),
                MultiGauge.Row.of(Tags.of("status", "IKKE_OPPDATERT_STATUS_ETTER_SENDT"), iverksettingerSendtForEnTimeSiden.size),
            ),
            true,
        )
    }
}
