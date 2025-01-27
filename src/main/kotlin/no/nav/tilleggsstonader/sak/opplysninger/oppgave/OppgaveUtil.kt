package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.tilleggsstonader.kontrakter.oppgave.IdentGruppe
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

object OppgaveUtil {
    private val logger = LoggerFactory.getLogger(javaClass)

    val ENHET_NR_NAY = "4462" // Tilleggsstønad INN
    val ENHET_NR_EGEN_ANSATT = "4483" // NAV Arbeid og ytelser Egne ansatte
    val ENHET_NR_STRENGT_FORTROLIG = "2103" // NAV Vikafossen

    fun sekunderSidenEndret(oppgave: Oppgave): Long? {
        val endretTidspunkt = oppgave.endretTidspunkt
        return if (!endretTidspunkt.isNullOrBlank()) {
            try {
                OffsetDateTime.parse(endretTidspunkt).until(OffsetDateTime.now(), ChronoUnit.SECONDS)
            } catch (e: Exception) {
                logger.warn("Feilet parsing av endretTidspunkt=$endretTidspunkt for opgave=$oppgave")
                null
            }
        } else {
            null
        }
    }

    fun finnPersonidentForOppgave(oppgave: Oppgave): String? = oppgave.identer?.first { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }?.ident

    fun utledBehandlesAvApplikasjon(oppgavetype: Oppgavetype) =
        when (oppgavetype) {
            Oppgavetype.Journalføring,
            Oppgavetype.BehandleSak,
            Oppgavetype.BehandleUnderkjentVedtak,
            Oppgavetype.GodkjenneVedtak,
            -> "tilleggsstonader-sak"

            else -> error("Håndterer ikke behandlesAvApplikasjon for $oppgavetype")
        }

    fun skalPlasseresIKlarMappe(oppgavetype: Oppgavetype) =
        when (oppgavetype) {
            Oppgavetype.Journalføring,
            Oppgavetype.BehandleSak,
            Oppgavetype.BehandleUnderkjentVedtak,
            Oppgavetype.GodkjenneVedtak,
            -> true

            else -> error("Håndterer ikke klar-mappe-håndtering for $oppgavetype")
        }

    /**
     * Skal ikke opprette oppgave når en behandling har feil status for gitt oppgavetype
     * Eks i en behandling som sendes til beslutter, så opprettes det en task for GodkjenneVedtak
     * Hvis saksbehandler angrer send til beslutter før oppgaven er opprettet, så skal man ikke opprette GodkjennVedtak-oppgaven
     */
    fun skalIkkeOppretteOppgave(
        saksbehandling: Saksbehandling,
        oppgavetype: Oppgavetype,
    ): Boolean {
        return when (oppgavetype) {
            Oppgavetype.BehandleSak -> saksbehandling.status.behandlingErLåstForVidereRedigering()
            Oppgavetype.GodkjenneVedtak -> saksbehandling.status != BehandlingStatus.FATTER_VEDTAK
            Oppgavetype.BehandleUnderkjentVedtak -> saksbehandling.status.behandlingErLåstForVidereRedigering()
            else -> {
                logger.warn("Har ikke håndtering av $oppgavetype for behandling=${saksbehandling.id}")
                return false
            }
        }
    }
}
