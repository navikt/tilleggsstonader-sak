package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import no.nav.tilleggsstonader.kontrakter.felles.Behandlingstema
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.oppgave.Behandlingstype
import no.nav.tilleggsstonader.kontrakter.oppgave.IdentGruppe
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveIdentV2
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveClient
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveDomain
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveRepository
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.time.LocalDate

/**
 * Oppretter oppgaver ut fra de som finnes i oppgaveRepository, for å populere oppgavebenken
 *
 * Den oppretter ikke oppgaver med riktig id.
 *  Det kan man eventuellt løse med å oppdatere OppgaveDomain etter opprettelse
 */
@Configuration
@Profile("opprett-oppgave")
class OpprettOppgaveConfig(
    private val oppgaveRepository: OppgaveRepository,
    private val behandlingService: BehandlingService,
    private val oppgaveClient: OppgaveClient,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val oppgavetyper =
        setOf(
            Oppgavetype.BehandleSak,
            Oppgavetype.GodkjenneVedtak,
            Oppgavetype.BehandleUnderkjentVedtak,
        )

    init {
        val oppgaver = oppgaveRepository.findAll()
            .filterNot { it.erFerdigstilt }
            .filter { oppgavetyper.contains(it.type) }
        oppgaver.forEach { oppgave ->
            val behandling = behandlingService.hentSaksbehandling(oppgave.behandlingId)
            val nyttOppgaveId = opprettOppgave(behandling, oppgave)
            // Oppdaterer oppgaveId på oppgaven då vi oppretter alle oppgaver på nytt, og får då et nytt oppgaveId
            oppgaveRepository.update(oppgave.copy(gsakOppgaveId = nyttOppgaveId))
        }
        logger.info("Opprettet ${oppgaver.size} oppgaver")
    }

    private fun opprettOppgave(
        behandling: Saksbehandling,
        oppgave: OppgaveDomain,
    ): Long {
        val oppgavetype = oppgave.type
        return oppgaveClient.opprettOppgave(
            OpprettOppgaveRequest(
                ident = OppgaveIdentV2(ident = behandling.ident, gruppe = IdentGruppe.FOLKEREGISTERIDENT),
                tema = mapTema(behandling.stønadstype),
                tilordnetRessurs = mapTilordnetRessurs(oppgavetype, behandling),
                oppgavetype = oppgavetype,
                behandlingstema = mapBehandlingstema(behandling.stønadstype).value,
                behandlingstype = Behandlingstype.NASJONAL.value,
                enhetsnummer = mapEnhet(),
                fristFerdigstillelse = LocalDate.now().plusDays(14),
                beskrivelse = mapBeskrivelse(oppgavetype),
                behandlesAvApplikasjon = "tilleggsstonader-sak",
            ),
        )
    }

    private fun mapEnhet() = "4462" // Tilleggsstønad INN, som er Nasjonal kø for NAY tilleggsstønader

    private fun mapTilordnetRessurs(
        oppgavetype: Oppgavetype,
        behandling: Saksbehandling,
    ) = if (oppgavetype == Oppgavetype.BehandleSak) behandling.opprettetAv else null

    private fun mapBeskrivelse(oppgavetype: Oppgavetype): String = when (oppgavetype) {
        Oppgavetype.BehandleSak -> "Behandle sak (opprettet når applikasjonen starter)"
        Oppgavetype.GodkjenneVedtak -> "Godkjenn vedtak (opprettet når applikasjonen starter)"
        Oppgavetype.BehandleUnderkjentVedtak -> "Behandle underkjent vedtak (opprettet når applikasjonen starter)"
        else -> error("Har ikke mappet $oppgavetype")
    }

    private fun mapTema(stønadstype: Stønadstype): Tema = when (stønadstype) {
        Stønadstype.BARNETILSYN -> Tema.TSO
    }

    private fun mapBehandlingstema(stønadstype: Stønadstype): Behandlingstema = when (stønadstype) {
        Stønadstype.BARNETILSYN -> Behandlingstema.Barnetilsyn
    }
}
