package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import no.nav.tilleggsstonader.sak.metrics.MetricUtil
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Iverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Multigauge som holder oversikt over antall iverksettinger hvor:
 * - Vi har mottatt feilet-status fra helved/utsjekk
 * - Vi har ikke mottatt status etter iverksetting
 */
@Component
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
                    StatusIverksetting.HOS_OPPDRAG,
                    StatusIverksetting.MOTTATT,
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

        val iverksettingerUtenOKStatus =
            sendteOgFeiledeAndeler
                .filter { it.statusIverksetting != StatusIverksetting.FEILET }
                .map {
                    StatusMedIverksetting(
                        status = it.statusIverksetting,
                        iverksetting = it.iverksetting!!, // alle som ikke har status UBEHANDLET har iverksetting
                    )
                }.distinct()
                .filter { skalVarsles(it) }
                .map { it.iverksetting }

        if (iverksettingerUtenOKStatus.isNotEmpty()) {
            logger.warn(
                "Iverksettinger uten OK-status: {}",
                iverksettingerUtenOKStatus,
            )
        }

        andelerSomHarIverksettingMedUgyldigStatusMultiGauge.register(
            listOf(
                MultiGauge.Row.of(Tags.of("status", "FEILET"), feiledeIverksettinger.size),
                MultiGauge.Row.of(Tags.of("status", "IKKE_MOTTATT_OK_STATUS"), iverksettingerUtenOKStatus.size),
            ),
            true,
        )
    }

    private fun skalVarsles(statusMedIverksetting: StatusMedIverksetting): Boolean {
        // Ved helg vil oppdrag ikke sende status, derfor venter vi til mandag
        return if (statusMedIverksetting.status == StatusIverksetting.HOS_OPPDRAG && oppdragErHelgestengt()) {
            false
        } else {
            statusMedIverksetting.iverksetting.iverksettingTidspunkt.isBefore(LocalDateTime.now().minus(Duration.ofHours(1)))
        }
    }

    /**
     * Hvis oppdrag er stengt og vi har iverksatt så vil status henge på HOS_OPPDRAG til oppdrag åpner igjen.
     * Forsøker å unngå unødvendig med varsler i helger.
     *
     * Oppdrag i utgangspunkt åpent 6-21 hverdager.
     * Oppdrag vil også være stengt ved helligdager, tar ikke høyde for dette
     *
     */
    private fun oppdragErHelgestengt() = erHelg() || erMandagFørKl06()

    private fun erMandagFørKl06(): Boolean {
        val nå = LocalDateTime.now()
        return nå.dayOfWeek == DayOfWeek.MONDAY && nå.hour < 6
    }

    private fun erHelg(): Boolean = LocalDate.now().dayOfWeek !in DayOfWeek.SATURDAY..DayOfWeek.SUNDAY

    private data class StatusMedIverksetting(
        val status: StatusIverksetting,
        val iverksetting: Iverksetting,
    )
}
