package no.nav.tilleggsstonader.sak.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Metrics
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.time.Instant

@Configuration
class AntallBehandlingerUtenOppgaveConfig(
    behandlingRepository: BehandlingRepository,
) {
    private val logger = LoggerFactory.getLogger(AntallBehandlingerUtenOppgaveConfig::class.java)

    var sistHentet = Instant.MIN
    var sisteVerdi = 0.0

    init {
        // Funksjonen her kalles for hver forspørsel på prometheus-endepunkt. Sørger for at v
        Gauge
            .builder<BehandlingRepository>("behandlinger_uten_oppgave", behandlingRepository) {
                // Slår ikke opp i databasen oftere enn hvert 30. minutt
                if (sistHentet.isBefore(Instant.now().minus(Duration.ofMinutes(30)))) {
                    val behandlingerUtenOppgave = it.finnBehandlingerUtenOppgave()
                    logger.info("Behandlinger uten oppgave: {}", behandlingerUtenOppgave)

                    sisteVerdi = behandlingerUtenOppgave.size.toDouble()
                    sistHentet = Instant.now()
                }

                sisteVerdi
            }.register(Metrics.globalRegistry)
    }
}
