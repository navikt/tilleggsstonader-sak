package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.BehandlerRolle
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.slf4j.LoggerFactory
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
    ) {
        tilgangService.validerTilgangTilRolle(BehandlerRolle.SAKSBEHANDLER)

        feilHvisIkke(unleashService.isEnabled(Toggle.HENT_BEHANDLINGER_FOR_OPPFØLGING)) {
            "Feature toggle ${Toggle.HENT_BEHANDLINGER_FOR_OPPFØLGING} er ikke aktivert"
        }

        oppfølgingService.kontroller(request)
    }

    @GetMapping("behandlinger")
    fun hentBehandlingerForOppfølging(): List<BehandlingForOppfølgingDto> {
        tilgangService.validerTilgangTilRolle(BehandlerRolle.VEILEDER)

        feilHvisIkke(unleashService.isEnabled(Toggle.HENT_BEHANDLINGER_FOR_OPPFØLGING)) {
            "Feature toggle ${Toggle.HENT_BEHANDLINGER_FOR_OPPFØLGING} er ikke aktivert"
        }

        return oppfølgingService.hentBehandlingerForOppfølging()
    }

    @PostMapping("start")
    fun startJobb() {
        tilgangService.validerTilgangTilRolle(BehandlerRolle.VEILEDER)
        Executors.newVirtualThreadPerTaskExecutor().submit {
            try {
                oppfølgingService.opprettTaskerForOppfølging()
            } catch (e: Exception) {
                logger.warn("Feilet start av oppfølgingjobb, se secure logs for flere detaljer")
                secureLogger.error("Feilet start av oppfølgingjobb", e)
            }
        }
    }
}
