package no.nav.tilleggsstonader.sak.statistikk.vedtak

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.familie.prosessering.util.MDCConstants
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.log.SecureLogger
import no.nav.tilleggsstonader.libs.log.logger
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.jboss.logging.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.Executors

@Tag(name = "Forvaltning")
@RestController
@RequestMapping("/api/forvaltning/vedtaksstatistikk")
@ProtectedWithClaims(issuer = "azuread")
class VedtaksstatistikkForvaltningController(
    private val tilgangService: TilgangService,
    private val vedtaksstatistikkService: VedtaksstatistikkService,
    private val behandlingRepository: BehandlingRepository,
    private val behandlingService: BehandlingService,
    private val personService: PersonService,
    private val transactionHandler: TransactionHandler,
) {
    @PostMapping("/oppdater/{behandlingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun oppdaterVedtaksstatistikk(
        @PathVariable behandlingId: BehandlingId,
    ) {
        tilgangService.validerHarUtviklerrolle()
        logger.info("Oppdaterer vedtaksstatistikk for behandling $behandlingId")
        vedtaksstatistikkService.oppdaterVedtaksstatistikkV2(behandlingId)
    }

    /**
     * Oppdaterer den eksisterende raden i vedtaksstatistikk-tabellen for alle behandlinger med
     * vedtak for gitt [stønadstype], i én transaksjon. Kjøres asynkront ettersom det kan ta tid
     * for stønadstyper med mye historikk.
     */
    @PostMapping("/oppdater-alle/{stønadstype}")
    fun oppdaterVedtaksstatistikkForAlleVedtatteBehandlinger(
        @PathVariable stønadstype: Stønadstype,
    ): ResponseEntity<String> {
        tilgangService.validerHarUtviklerrolle()
        logger.info("Starter oppdatering av vedtaksstatistikk for stønadstype {}", stønadstype)

        val callId = MDC.get(MDCConstants.MDC_CALL_ID)
        Executors.newVirtualThreadPerTaskExecutor().submit {
            MDC.put(MDCConstants.MDC_CALL_ID, callId)
            try {
                transactionHandler.runInNewTransaction {
                    oppdaterVedtaksstatistikkForStønadstype(stønadstype)
                }
            } catch (e: Exception) {
                SecureLogger.secureLogger.error("Feilet oppdatering av vedtaksstatistikk for stønadstype $stønadstype", e)
            } finally {
                MDC.remove(MDCConstants.MDC_CALL_ID)
            }
        }
        return ResponseEntity.ok("Oppdatering av vedtaksstatistikk for $stønadstype kjører!")
    }

    private fun oppdaterVedtaksstatistikkForStønadstype(stønadstype: Stønadstype) {
        val behandlingIder = behandlingRepository.finnBehandlingerMedVedtak(stønadstype)

        // Bygger opp PDL-cache så vi slipper å gjøre ett kall per person
        personService.hentPersonKortBolk(
            behandlingIder.map { behandlingService.hentAktivIdent(it) },
        )

        behandlingIder.forEach { behandlingId ->
            vedtaksstatistikkService.oppdaterVedtaksstatistikkV2(behandlingId)
        }

        logger.info("Oppdatering av vedtaksstatistikk for stønadstype {} ferdig", stønadstype)
    }
}
