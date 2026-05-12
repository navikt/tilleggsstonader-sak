package no.nav.tilleggsstonader.sak.privatbil.varsel

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/**
 * Scheduled service som sender varsler om tilgjengelige kjørelister til brukere.
 * Kjører mandag kl 10 og varsler brukere som har rammevedtak gjeldende for forrige kalenderuke.
 */
@Service
class KjørelistevarselScheduledJobb(
    private val kjørelistevarselService: KjørelistevarselService,
) {
    /**
     * Kjører mandag kl 10
     * Cron format: <sekund> <minutt> <time> <dag i måned> <måned> <dag i uke>
     */
    @Scheduled(cron = "0 0 10 * * MON") // kl. 10:00
    @SchedulerLock(name = "sendVarselOmKjørelister", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    fun sendVarselOmKjørelister() {
        kjørelistevarselService.sendUkentligVarselOmKjørelister()
    }
}
