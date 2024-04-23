package no.nav.tilleggsstonader.sak.opplysninger.kodeverk

import no.nav.tilleggsstonader.libs.log.mdc.MDCConstants
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.cache.CacheManager
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Henter kodeverk på nytt hver natt kl 1. Dette for å unngå at vi må hente det synkront når noen trenger dataen.
 * Disse blir cachet i [CachedKodeverkService]
 */
@Component
@Profile("!integrasjonstest")
class KodeverkInitializer(
    private val kodeverkService: CachedKodeverkService,
    @Qualifier("kodeverkCache")
    private val cacheManager: CacheManager,
) : ApplicationListener<ApplicationReadyEvent> {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = HVER_DAG_KL_01)
    fun syncKodeverk() {
        logger.info("Kjører schedulert jobb for å hente kodeverk")
        cacheManager.cacheNames.stream().forEach { cacheManager.getCache(it)?.clear() }
        sync()
    }

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        sync()
    }

    private fun sync() {
        syncKodeverk("Poststed", kodeverkService::hentPoststed)
        syncKodeverk("Landkoder", kodeverkService::hentLandkoder)
    }

    private fun syncKodeverk(navn: String, henter: () -> Unit) {
        try {
            MDC.put(MDCConstants.MDC_CALL_ID, UUID.randomUUID().toString())
            logger.info("Henter kodeverk=$navn")
            henter.invoke()
            logger.info("Hentet kodeverk=$navn")
        } catch (e: Exception) {
            logger.warn("Feilet henting av kodeverk=$navn ${e.message}")
        } finally {
            MDC.clear()
        }
    }

    companion object {
        private const val HVER_DAG_KL_01 = "0 0 1 * * *"
    }
}
