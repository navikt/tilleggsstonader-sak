package no.nav.tilleggsstonader.sak.migrering.arena

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.EksternApplikasjon
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.util.EnvUtil.erIProd
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Brukes av Arena for å finne ut om en person finnes i ny løsning.
 * Hvis den finnes så skal personen låses fra div operasjoner i arena.
 */
@RestController
@RequestMapping(path = ["/api/ekstern/arena/status"])
@ProtectedWithClaims(issuer = "azuread")
class EksternArenaController(
    private val arenaStatusService: ArenaStatusService,
) {

    @PostMapping
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun hentStatusTilArena(@RequestBody request: ArenaFinnesPersonRequest): ArenaFinnesPersonResponse {
        feilHvisIkke(SikkerhetContext.kallKommerFra(EksternApplikasjon.ARENA), HttpStatus.UNAUTHORIZED) {
            "Kallet utføres ikke av en autorisert klient"
        }

        try {
            return arenaStatusService.finnStatus(request)
        } catch (e: Exception) {
            if (!erIProd() && e.message == "Finner ikke mapping for AAP") {
                /**
                 * Spesialhåndtering i test fordi Arena tester tjenesten med rettighet=AAP som ikke eksisterer
                 * Kaster [ApiFeil] som logger info i stedet for error, med INTERNAL_SERVER_ERROR som skjer ellers
                 */
                throw ApiFeil("Finner ikke mapping for AAP", HttpStatus.INTERNAL_SERVER_ERROR)
            } else {
                throw e
            }
        }
    }

    @PostMapping("bruker")
    @ProtectedWithClaims(issuer = "azuread")
    fun hentArenaStatus(@RequestBody request: ArenaFinnesPersonRequest): ArenaFinnesPersonResponse {
        try {
            return arenaStatusService.finnStatus(request)
        } catch (e: Exception) {
            if (!erIProd() && e.message == "Finner ikke mapping for AAP") {
                /**
                 * Spesialhåndtering i test fordi Arena tester tjenesten med rettighet=AAP som ikke eksisterer
                 * Kaster [ApiFeil] som logger info i stedet for error, med INTERNAL_SERVER_ERROR som skjer ellers
                 */
                throw ApiFeil("Finner ikke mapping for AAP", HttpStatus.INTERNAL_SERVER_ERROR)
            } else {
                throw e
            }
        }
    }
}
