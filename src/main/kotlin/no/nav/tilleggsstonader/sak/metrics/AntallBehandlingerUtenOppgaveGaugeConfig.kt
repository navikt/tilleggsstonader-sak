package no.nav.tilleggsstonader.sak.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Metrics
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveRepository
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveUtil.GYLDIGE_ENHETER_TILLEGGSTØNADER
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.time.Instant

@Configuration
class AntallBehandlingerUtenOppgaveGaugeConfig(
    behandlingRepository: BehandlingRepository,
    oppgaveRepository: OppgaveRepository,
) {
    private val logger = LoggerFactory.getLogger(AntallBehandlingerUtenOppgaveGaugeConfig::class.java)

    var sistHentetBehandlingerUtenOppgave = Instant.MIN
    var sisteVerdiBehandlingerUtenOppgave = 0.0

    var sistHentetOppgaverTildeltUkjentEnhet = Instant.MIN
    var sisteVerdiOppgaverTildeltUkjentEnhet = 0.0

    init {
        // Funksjonen her kalles for hver forspørsel på prometheus-endepunkt. Cacher derfor svaret for å unngå unødvendig mange db-spørringer
        Gauge
            .builder("behandlinger_uten_oppgave", behandlingRepository) {
                // Slår ikke opp i databasen oftere enn hvert 30. minutt
                if (sistHentetBehandlingerUtenOppgave.isBefore(Instant.now().minus(Duration.ofMinutes(30)))) {
                    val behandlingerUtenOppgave =
                        it.finnBehandlingerUtenÅpenOppgave()
                    if (behandlingerUtenOppgave.isNotEmpty()) {
                        logger.info("Behandlinger uten oppgave: {}", behandlingerUtenOppgave)
                    }

                    sisteVerdiBehandlingerUtenOppgave = behandlingerUtenOppgave.size.toDouble()
                    sistHentetBehandlingerUtenOppgave = Instant.now()
                }

                sisteVerdiBehandlingerUtenOppgave
            }.register(Metrics.globalRegistry)

        Gauge
            .builder("oppgaver_tildelt_ukjent_enhet", oppgaveRepository) {
                // Slår ikke opp i databasen oftere enn hvert 30. minutt
                if (sistHentetOppgaverTildeltUkjentEnhet.isBefore(Instant.now().minus(Duration.ofMinutes(30)))) {
                    val oppgaverTildeltUkjentEnhet =
                        it.finnÅpneBehandlingsoppgaverIkkeTildeltEnhet(GYLDIGE_ENHETER_TILLEGGSTØNADER)
                    if (oppgaverTildeltUkjentEnhet.isNotEmpty()) {
                        logger.info(
                            "Behandlinger som har oppgave tildelt ukjent enhet: {}",
                            oppgaverTildeltUkjentEnhet.map { o -> o.behandlingId },
                        )
                    }

                    sisteVerdiOppgaverTildeltUkjentEnhet = oppgaverTildeltUkjentEnhet.size.toDouble()
                    sistHentetOppgaverTildeltUkjentEnhet = Instant.now()
                }

                sisteVerdiOppgaverTildeltUkjentEnhet
            }.register(Metrics.globalRegistry)
    }
}
