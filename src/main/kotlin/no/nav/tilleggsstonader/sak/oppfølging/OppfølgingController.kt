package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.familie.prosessering.util.MDCConstants
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.BehandlerRolle
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.Executors

@RestController
@RequestMapping(path = ["/api/oppfolging"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OppfølgingController(
    private val tilgangService: TilgangService,
    private val oppfølgingService: OppfølgingService,
    private val unleashService: UnleashService,
    private val oppfølgingRepository: OppfølgingRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun hentAktiveOppfølginger(): List<OppfølgingMedDetaljer> {
        tilgangService.validerTilgangTilRolle(BehandlerRolle.VEILEDER)

        feilHvisIkke(unleashService.isEnabled(Toggle.HENT_BEHANDLINGER_FOR_OPPFØLGING)) {
            "Feature toggle ${Toggle.HENT_BEHANDLINGER_FOR_OPPFØLGING} er ikke aktivert"
        }

        return oppfølgingService.hentAktiveOppfølginger()
    }

    @PostMapping("kontroller")
    fun kontrollerBehandling(
        @RequestBody request: KontrollerOppfølgingRequest,
    ): OppfølgingMedDetaljer {
        tilgangService.validerTilgangTilRolle(BehandlerRolle.SAKSBEHANDLER)

        feilHvisIkke(unleashService.isEnabled(Toggle.HENT_BEHANDLINGER_FOR_OPPFØLGING)) {
            "Feature toggle ${Toggle.HENT_BEHANDLINGER_FOR_OPPFØLGING} er ikke aktivert"
        }

        return oppfølgingService.kontroller(request)
    }

    @PostMapping("start")
    fun startJobb() {
        tilgangService.validerTilgangTilRolle(BehandlerRolle.VEILEDER)
        val callId = MDC.get(MDCConstants.MDC_CALL_ID)
        Executors.newVirtualThreadPerTaskExecutor().submit {
            try {
                MDC.put(MDCConstants.MDC_CALL_ID, callId)
                oppfølgingService.opprettTaskerForOppfølging()
            } catch (e: Exception) {
                logger.warn("Feilet start av oppfølgingjobb, se secure logs for flere detaljer")
                secureLogger.error("Feilet start av oppfølgingjobb", e)
            } finally {
                MDC.remove(MDCConstants.MDC_CALL_ID)
            }
        }
    }

    @PostMapping("reset-state")
    fun fjernAlle() {
        tilgangService.validerTilgangTilRolle(BehandlerRolle.VEILEDER)
        oppfølgingRepository.deleteAll()
    }
}
