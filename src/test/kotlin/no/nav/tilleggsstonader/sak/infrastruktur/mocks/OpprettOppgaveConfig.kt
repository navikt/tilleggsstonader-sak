package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import no.nav.tilleggsstonader.kontrakter.felles.tilBehandlingstema
import no.nav.tilleggsstonader.kontrakter.felles.tilTema
import no.nav.tilleggsstonader.kontrakter.oppgave.Behandlingstype
import no.nav.tilleggsstonader.kontrakter.oppgave.IdentGruppe
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveIdentV2
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveMappe
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveDomain
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveRepository
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveUtil.utledBehandlesAvApplikasjon
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
    private val oppgaveService: OppgaveService,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val oppgavelager: Oppgavelager,
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
        val oppgaverUnderArbeid =
            oppgaver
                .filterNot { it.erFerdigstilt }
                .filter { oppgavetyper.contains(it.type) }
        oppgaverUnderArbeid.forEach { oppgave ->
            oppgave.behandlingId?.let { behandlingId ->
                val behandling = behandlingService.hentSaksbehandling(behandlingId)
                opprettOppgave(behandling, oppgave)
            }
        }
        oppgavelager.oppdaterMaxOppgaveId(oppgaver.maxOfOrNull { it.gsakOppgaveId })
        logger.info("Opprettet ${oppgaverUnderArbeid.size} oppgaver")
    }

    private fun opprettOppgave(
        behandling: Saksbehandling,
        oppgave: OppgaveDomain,
    ): Long {
        val oppgavetype = oppgave.type
        return oppgavelager
            .leggTilOppgaveFraRepository(
                OpprettOppgaveRequest(
                    ident = OppgaveIdentV2(ident = behandling.ident, gruppe = IdentGruppe.FOLKEREGISTERIDENT),
                    tema = behandling.stønadstype.tilTema(),
                    tilordnetRessurs = mapTilordnetRessurs(oppgavetype, behandling),
                    oppgavetype = oppgavetype,
                    behandlingstema = behandling.stønadstype.tilBehandlingstema().value,
                    behandlingstype = Behandlingstype.NASJONAL.value,
                    enhetsnummer = mapEnhet(),
                    fristFerdigstillelse = LocalDate.now().plusDays(14),
                    beskrivelse = mapBeskrivelse(oppgavetype),
                    behandlesAvApplikasjon = utledBehandlesAvApplikasjon(oppgavetype),
                    mappeId = oppgaveService.finnMappe(mapEnhet(), OppgaveMappe.KLAR).id,
                ),
                oppgave.gsakOppgaveId,
            ).id
    }

    private fun mapEnhet() = "4462" // Tilleggsstønad INN, som er Nasjonal kø for NAY tilleggsstønader

    private fun mapTilordnetRessurs(
        oppgavetype: Oppgavetype,
        behandling: Saksbehandling,
    ) = if (oppgavetype == Oppgavetype.BehandleSak) behandling.opprettetAv else null

    private fun mapBeskrivelse(oppgavetype: Oppgavetype): String =
        when (oppgavetype) {
            Oppgavetype.BehandleSak -> "Behandle sak (opprettet når applikasjonen starter)"
            Oppgavetype.GodkjenneVedtak -> "Godkjenn vedtak (opprettet når applikasjonen starter)"
            Oppgavetype.BehandleUnderkjentVedtak -> "Behandle underkjent vedtak (opprettet når applikasjonen starter)"
            else -> error("Har ikke mappet $oppgavetype")
        }
}
