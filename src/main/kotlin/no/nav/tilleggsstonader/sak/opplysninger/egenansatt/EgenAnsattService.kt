package no.nav.tilleggsstonader.sak.opplysninger.egenansatt

import no.nav.tilleggsstonader.sak.infrastruktur.config.getCachedOrLoad
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service

@Service
class EgenAnsattService(
    private val egenAnsattClient: EgenAnsattClient,
    private val cacheManager: CacheManager,
) {
    fun erEgenAnsatt(personIdent: String): Boolean = erEgenAnsatt(setOf(personIdent)).values.single()

    fun erEgenAnsatt(personIdenter: Set<String>): Map<String, Boolean> =
        cacheManager.getCachedOrLoad("erEgenAnsattBolk", personIdenter) {
            egenAnsattClient.erEgenAnsatt(personIdenter)
        }
}
