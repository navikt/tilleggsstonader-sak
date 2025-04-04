package no.nav.tilleggsstonader.sak.opplysninger.kodeverk

import no.nav.tilleggsstonader.kontrakter.kodeverk.KodeverkDto
import no.nav.tilleggsstonader.kontrakter.kodeverk.hentGjeldende
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * Henter cachade verdier fra kodeverk. Kodeverk inneholder mapping av div verdier til norsk tekst av verdiet.
 * Eks inneholder kodeverk mapping fra postnummer 0010 til Oslo
 */
@Service
class KodeverkService(
    private val cachedKodeverkService: CachedKodeverkService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentPoststed(postnummer: String?) =
        hentKodeverdi("poststed", postnummer) {
            cachedKodeverkService.hentPoststed().hentGjeldende(it)
        }

    /**
     * Henter land basert ISO3-landkode
     */
    fun hentLandkode(landkodeIso3: String?) =
        hentKodeverdi("landkode", landkodeIso3) {
            cachedKodeverkService.hentLandkoder().hentGjeldende(it)
        }

    /**
     * Henter land basert ISO2-landkode
     */
    fun hentLandkodeIso2(landkodeIso2: String?) =
        hentKodeverdi("landkode", landkodeIso2) {
            cachedKodeverkService.hentLandkoderIso2().hentGjeldende(it)
        }

    private fun hentKodeverdi(
        type: String,
        kode: String?,
        hentKodeverdiFunction: Function1<String, String?>,
    ): String =
        try {
            kode?.let(hentKodeverdiFunction) ?: kode ?: ""
        } catch (e: Exception) {
            // Ikke la feil kodeverk stoppe henting av data
            logger.error("Feilet henting av $type til $kode message=${e.message} cause=${e.cause?.message}")
            ""
        }
}

@Service
@CacheConfig(cacheManager = "kodeverkCache")
class CachedKodeverkService(
    private val kodeverkClient: KodeverkClient,
) {
    @Cacheable("poststed", sync = true)
    fun hentPoststed(): KodeverkDto = kodeverkClient.hentPostnummer()

    @Cacheable("landkoder", sync = true)
    fun hentLandkoder(): KodeverkDto = kodeverkClient.hentLandkoder()

    @Cacheable("landkoderIso2", sync = true)
    fun hentLandkoderIso2(): KodeverkDto = kodeverkClient.hentLandkoderIso2()
}
