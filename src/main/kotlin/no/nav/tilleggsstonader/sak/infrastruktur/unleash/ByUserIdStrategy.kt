package no.nav.tilleggsstonader.sak.infrastruktur.unleash

import io.getunleash.strategy.Strategy
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Component

@Component
class ByUserIdStrategy : Strategy {
    override fun getName(): String = "userWithId"

    override fun isEnabled(map: MutableMap<String, String>): Boolean =
        map["userIds"]
            ?.split(',')
            ?.any { SikkerhetContext.hentSaksbehandler().equals(it, ignoreCase = true) }
            ?: false
}
