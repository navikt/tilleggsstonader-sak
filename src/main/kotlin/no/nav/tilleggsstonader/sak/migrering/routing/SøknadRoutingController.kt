package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.IdentStønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.EksternApplikasjon
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.pdl.logger
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/ekstern"])
@ProtectedWithClaims(issuer = "azuread")
class SøknadRoutingController(
    private val søknadRoutingService: SøknadRoutingService,
    private val fagsakService: FagsakService,
) {

    @PostMapping("routing-soknad")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun sjekkRoutingForPerson(@RequestBody request: IdentStønadstype): SøknadRoutingResponse {
        feilHvisIkke(SikkerhetContext.kallKommerFra(EksternApplikasjon.SOKNAD_API), HttpStatus.UNAUTHORIZED) {
            "Kallet utføres ikke av en autorisert klient"
        }
        return søknadRoutingService.sjekkRoutingForPerson(request)
    }


    @PostMapping("harBehandling")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun hentBehandlingStatusForPersonMedStønadstype(@RequestBody identStønadstype: IdentStønadstype): Boolean {
        //tilgangService.validerTilgangTilPersonMedBarn(identStønadstype.ident, AuditLoggerEvent.ACCESS)
        logger.info("hello the fødselsnummmer is fødselsnummer" +identStønadstype.ident,)
        logger.info("hello the fødselsnummmer is fødselsnummer" +identStønadstype.stønadstype,)
        val behandlinger = fagsakService.hentBehandlingerForPersonOgStønadstype(
            identStønadstype.ident,
            identStønadstype.stønadstype,
        )
        logger.info("behandlinger is" +behandlinger[0])
        if (behandlinger.isNotEmpty()) {
            val validStatuses = listOf(
                BehandlingStatus.OPPRETTET,
                BehandlingStatus.UTREDES,
                BehandlingStatus.FATTER_VEDTAK,
                BehandlingStatus.SATT_PÅ_VENT,
            )
            val behandlingStatus = behandlinger.map { it.status }
            logger.info("behandlinger status" +behandlingStatus)
            return behandlingStatus.any { it in validStatuses }
        }
        return false
    }
}

