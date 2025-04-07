package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.libs.log.mdc.MDCConstants
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.Executors

@RestController
@RequestMapping(path = ["/api/oppfolging/admin"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OppfølgingAdminController(
    private val oppfølgingRepository: OppfølgingRepository,
    private val transactionHandler: TransactionHandler,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun migrer() {
        val callId = MDC.get(MDCConstants.MDC_CALL_ID)
        Executors.newVirtualThreadPerTaskExecutor().submit {
            MDC.put(MDCConstants.MDC_CALL_ID, callId)
            try {
                transactionHandler.runInTransaction { oppfølgingRepository.findAll().forEach { handle(it) } }
            } catch (e: Throwable) {
                logger.warn("Feil ved oppdatering. Se securelogs")
                secureLogger.error("Feil ved oppdatering", e)
            }
        }
    }

    fun handle(oppfølging: Oppfølging) {
        val perioderTilKontroll =
            oppfølging.data.perioderTilKontroll.flatMap { kontroll ->
                val endringAktiviteter =
                    kontroll.endringAktivitet!!.takeIf { it.isNotEmpty() }?.let {
                        PeriodeForKontroll(
                            fom = kontroll.fom,
                            tom = kontroll.tom,
                            type = kontroll.aktivitet!!,
                            endringer = it,
                        )
                    }
                val endringMålgrupper =
                    kontroll.endringMålgruppe!!.takeIf { it.isNotEmpty() }?.let {
                        PeriodeForKontroll(
                            fom = kontroll.fom,
                            tom = kontroll.tom,
                            type = kontroll.målgruppe!!,
                            endringer = it,
                        )
                    }
                listOfNotNull(endringAktiviteter, endringMålgrupper)
            }
        val oppdatertOppfølging =
            oppfølging.copy(data = oppfølging.data.copy(perioderTilKontroll = perioderTilKontroll))
        oppfølgingRepository.update(oppdatertOppfølging)
    }
}
