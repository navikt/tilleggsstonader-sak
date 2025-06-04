package no.nav.tilleggsstonader.sak.beregnfra

import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.Executors

@RestController
@RequestMapping(path = ["/api/beregnfra"])
@Unprotected
class BeregnFraSammenlignerController(
    private val beregnFraSammenligner: BeregnFraSammenligner,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun startSammenligning() {
        Executors.newVirtualThreadPerTaskExecutor().submit {
            try {
                beregnFraSammenligner.sammenlignRevurderFraMedBeregnFra()
            } catch (e: Exception) {
                logger.warn("Feil under sammenligning av revurderinger med beregn fra", e)
            }
        }
    }
}
