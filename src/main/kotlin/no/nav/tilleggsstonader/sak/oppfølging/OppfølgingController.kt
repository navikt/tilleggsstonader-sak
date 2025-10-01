package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.familie.prosessering.util.MDCConstants
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.BehandlerRolle
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gjeldende
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.visningsnavn
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.Executors

@RestController
@RequestMapping(path = ["/api/oppfolging"])
@ProtectedWithClaims(issuer = "azuread")
class OppfølgingController(
    private val tilgangService: TilgangService,
    private val oppfølgingService: OppfølgingService,
    private val oppfølgingOpprettKontrollerService: OppfølgingOpprettKontrollerService,
    private val unleashService: UnleashService,
    private val personService: PersonService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun hentAktiveOppfølginger(): List<KontrollerOppfølgingResponse> {
        tilgangService.validerTilgangTilRolle(BehandlerRolle.VEILEDER)

        feilHvisIkke(unleashService.isEnabled(Toggle.HENT_BEHANDLINGER_FOR_OPPFØLGING)) {
            "Feature toggle ${Toggle.HENT_BEHANDLINGER_FOR_OPPFØLGING} er ikke aktivert"
        }

        return oppfølgingService.hentAktiveOppfølginger().tilDto()
    }

    @PostMapping("kontroller")
    fun kontrollerBehandling(
        @RequestBody request: KontrollerOppfølgingRequest,
    ): KontrollerOppfølgingResponse {
        tilgangService.validerTilgangTilRolle(BehandlerRolle.SAKSBEHANDLER)

        feilHvisIkke(unleashService.isEnabled(Toggle.HENT_BEHANDLINGER_FOR_OPPFØLGING)) {
            "Feature toggle ${Toggle.HENT_BEHANDLINGER_FOR_OPPFØLGING} er ikke aktivert"
        }

        return oppfølgingService.kontroller(request).let {
            it.tilDto(
                navn = personService.hentVisningsnavnForPerson(it.behandlingsdetaljer.fagsakPersonIdent),
            )
        }
    }

    @PostMapping("start")
    fun startJobb() {
        tilgangService.validerTilgangTilRolle(BehandlerRolle.VEILEDER)
        val callId = MDC.get(MDCConstants.MDC_CALL_ID)
        Executors.newVirtualThreadPerTaskExecutor().submit {
            try {
                MDC.put(MDCConstants.MDC_CALL_ID, callId)
                oppfølgingOpprettKontrollerService.opprettTaskerForOppfølging()
            } catch (e: Exception) {
                logger.warn("Feilet start av oppfølgingjobb, se secure logs for flere detaljer")
                secureLogger.error("Feilet start av oppfølgingjobb", e)
            } finally {
                MDC.remove(MDCConstants.MDC_CALL_ID)
            }
        }
    }

    private fun Collection<OppfølgingMedDetaljer>.tilDto(): List<KontrollerOppfølgingResponse> {
        val identer = this.map { it.behandlingsdetaljer.fagsakPersonIdent }.distinct()
        val identerMedNavn = hentNavnForIdenter(identer)

        return this.map { it.tilDto(identerMedNavn[it.behandlingsdetaljer.fagsakPersonIdent] ?: "") }
    }

    private fun OppfølgingMedDetaljer.tilDto(navn: String) =
        KontrollerOppfølgingResponse(
            id = id,
            behandlingId = behandlingId,
            version = version,
            opprettetTidspunkt = opprettetTidspunkt,
            perioderTilKontroll = data.perioderTilKontroll,
            kontrollert = kontrollert,
            behandlingsdetaljer = behandlingsdetaljer.tilDto(navn),
        )

    private fun Behandlingsdetaljer.tilDto(navn: String): OppfølgingBehandlingDetaljerDto =
        OppfølgingBehandlingDetaljerDto(
            saksnummer = saksnummer,
            fagsakPersonId = fagsakPersonId,
            fagsakPersonIdent = fagsakPersonIdent,
            fagsakPersonNavn = navn,
            stønadstype = stønadstype,
            vedtakstidspunkt = vedtakstidspunkt,
            harNyereBehandling = harNyereBehandling,
        )

    private fun hentNavnForIdenter(identer: List<String>): Map<String, String> =
        personService.hentPersonKortBolk(identer).mapValues {
            it.value.navn
                .gjeldende()
                .visningsnavn()
        }
}
