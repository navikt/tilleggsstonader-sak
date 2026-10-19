package no.nav.tilleggsstonader.sak.oppfølging

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.tilTema
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class OppfølgingScheduledJobb(
    private val oppfølgingOpprettKontrollerService: OppfølgingOpprettKontrollerService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 22 * * SUN", zone = "Europe/Oslo")
    @SchedulerLock(name = "opprettOppfølgingTasker", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    fun opprettTaskerForOppfølging() {
        Stønadstype.entries
            .map { it.tilTema() }
            .distinct()
            .forEach { tema ->
                try {
                    oppfølgingOpprettKontrollerService.opprettTaskerForOppfølging(tema)
                } catch (e: Exception) {
                    logger.error("Feilet start av oppfølgingjobb, se secure logs for flere detaljer")
                    secureLogger.error("Feilet start av oppfølgingjobb for tema=$tema", e)
                }
            }
    }
}
