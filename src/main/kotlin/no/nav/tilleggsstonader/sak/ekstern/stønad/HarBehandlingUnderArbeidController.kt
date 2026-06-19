package no.nav.tilleggsstonader.sak.ekstern.stønad

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.IdentSkjematype
import no.nav.tilleggsstonader.kontrakter.felles.IdentStønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Skjematype
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.EksternApplikasjon
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// TODO: Fjern stønadstype-feltet og gå over til IdentSkjematype som DTO når soknad-api er ferdig migrert til å bruke skjemakode.
data class HarBehandlingRequest(
    val ident: String,
    val stønadstype: Stønadstype? = null,
    val skjematype: Skjematype? = null,
)

@RestController
@RequestMapping(
    path = ["/api/ekstern/har-behandling"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
class HarBehandlingUnderArbeidController(
    private val harBehandlingUnderArbeidService: HarBehandlingUnderArbeidService,
    private val eksternApplikasjon: EksternApplikasjon,
) {
    @PostMapping()
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun harBehandlingUnderArbeid(
        @RequestBody request: HarBehandlingRequest,
    ): Boolean {
        feilHvisIkke(SikkerhetContext.kallKommerFra(eksternApplikasjon.soknadApi), HttpStatus.UNAUTHORIZED) {
            "Kallet utføres ikke av en autorisert klient"
        }
        // TODO: Midlertidig godtas både skjematype og stønadstype mens vi migrerer sokand-api over til å bruke skjemakode.
        if (request.skjematype != null) {
            return harBehandlingUnderArbeidService.harSøknadUnderBehandling(
                identSkjematype = IdentSkjematype(request.ident, request.skjematype),
            )
        }
        val stønadstype = request.stønadstype ?: feil("Enten stønadstype eller skjematype må oppgis")
        return harBehandlingUnderArbeidService.harSøknadUnderBehandling(IdentStønadstype(request.ident, stønadstype))
    }
}
