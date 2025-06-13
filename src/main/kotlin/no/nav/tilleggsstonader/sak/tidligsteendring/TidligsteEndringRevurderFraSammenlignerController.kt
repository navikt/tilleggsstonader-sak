package no.nav.tilleggsstonader.sak.tidligsteendring

import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.Executors

@RestController
@RequestMapping(path = ["/api/tidligste-endring"])
@Unprotected
class TidligsteEndringRevurderFraSammenlignerController(
    private val tidligsteEndringRevurderFraSammenligner: TidligsteEndringRevurderFraSammenligner,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun startSammenligning() {
        Executors.newVirtualThreadPerTaskExecutor().submit {
            try {
                tidligsteEndringRevurderFraSammenligner.sammenlignRevurderFraMedTidligsteEndring()
            } catch (e: Exception) {
                logger.warn("Feil under sammenligning av revurderinger med tidligste endring", e)
            }
        }
    }
}
