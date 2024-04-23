package no.nav.tilleggsstonader.sak.infrastruktur.config

import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.RolleConfig
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@ComponentScan("no.nav.familie.prosessering")
@Configuration
class TaskProsesseringConfig(
    private val rolleConfig: RolleConfig,
) {

    @Bean
    fun prosesseringInfoProvider() = object : ProsesseringInfoProvider {
        override fun hentBrukernavn(): String = try {
            SpringTokenValidationContextHolder().getTokenValidationContext().getClaims("azuread")
                .getStringClaim("preferred_username")
        } catch (e: Exception) {
            throw Feil("Mangler preferred_username på request")
        }

        override fun harTilgang(): Boolean {
            return SikkerhetContext.hentGrupperFraToken().contains(rolleConfig.prosessering)
        }
    }
}
