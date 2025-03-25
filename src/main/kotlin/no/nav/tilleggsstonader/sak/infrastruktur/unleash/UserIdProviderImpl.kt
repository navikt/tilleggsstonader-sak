package no.nav.tilleggsstonader.sak.infrastruktur.unleash

import no.nav.tilleggsstonader.libs.unleash.UserIdProvider
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Component

@Component
class UserIdProviderImpl : UserIdProvider {
    override fun userId(): String = SikkerhetContext.hentSaksbehandler()
}
