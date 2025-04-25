package no.nav.tilleggsstonader.sak.infrastruktur

import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.FaktaGrunnlagService
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * Fra https://github.com/navikt/tilleggsstonader-sak/pull/261
 * så blir grunnlagsdata lagt til når man går inn på en behandling første gang.
 * For at behandlinger som allerede har en annen status enn OPPRETTET skal få grunnlagsdata må det opprettes manuelt
 */
@Configuration
@Profile("opprett-grunnlagsdata")
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class OpprettGrunnlagsdataLokalPostgres(
    behandlingRepository: BehandlingRepository,
    faktaGrunnlagService: FaktaGrunnlagService,
) {
    init {
        var antallFeilet = 0
        behandlingRepository.findAll().forEach {
            if (it.status != BehandlingStatus.OPPRETTET) {
                try {
                    faktaGrunnlagService.opprettGrunnlagHvisDetIkkeEksisterer(it.id)
                } catch (e: Exception) {
                    antallFeilet++
                    secureLogger.warn("Feilet opprettelse av grunnlagsdata til behandling=${it.id}", e)
                }
            }
        }
        secureLogger.info("Opprettet grunnlagsdata antallFeil=$antallFeilet")
    }
}
