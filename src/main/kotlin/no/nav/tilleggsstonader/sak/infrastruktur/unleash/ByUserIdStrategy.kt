package no.nav.tilleggsstonader.sak.infrastruktur.unleash

import io.getunleash.strategy.Strategy
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Component

@Component
class ByUserIdStrategy : Strategy {

    override fun getName(): String {
        return "userWithId"
    }

    override fun isEnabled(map: MutableMap<String, String>): Boolean {
        return map["userIds"]
            ?.split(',')
            ?.any { SikkerhetContext.hentSaksbehandler() == it }
            ?: false
    }
}
