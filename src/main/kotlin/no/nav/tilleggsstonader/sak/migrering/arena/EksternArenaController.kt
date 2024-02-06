package no.nav.tilleggsstonader.sak.migrering.arena

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.EksternApplikasjon
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
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
        try {
            feilHvisIkke(SikkerhetContext.kallKommerFra(EksternApplikasjon.ARENA), HttpStatus.UNAUTHORIZED) {
                "Kallet utføres ikke av en autorisert klient"
            }
        } catch (e: Exception) {
            // TODO slett dette, vet ikke hva navnet på applikasjonen er
        }
        return arenaStatusService.finnStatus(request)
    }
}
