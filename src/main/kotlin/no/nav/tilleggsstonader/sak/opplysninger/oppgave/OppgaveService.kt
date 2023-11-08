package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.tilleggsstonader.kontrakter.felles.Behandlingstema
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnOppgaveRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnOppgaveResponseDto
import no.nav.tilleggsstonader.kontrakter.oppgave.IdentGruppe
import no.nav.tilleggsstonader.kontrakter.oppgave.MappeDto
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveIdentV2
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgavePrioritet
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.config.getValue
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class OppgaveService(
    private val oppgaveClient: OppgaveClient,
    private val fagsakService: FagsakService,
    private val oppgaveRepository: OppgaveRepository,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val cacheManager: CacheManager,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto {
        return oppgaveClient.hentOppgaver(finnOppgaveRequest)
    }

    fun fordelOppgave(gsakOppgaveId: Long, saksbehandler: String?, versjon: Int): Oppgave {
        return oppgaveClient.fordelOppgave(
            gsakOppgaveId,
            saksbehandler,
            versjon,
        )
    }

    fun opprettOppgave(
        behandlingId: UUID,
        oppgavetype: Oppgavetype,
        tilordnetNavIdent: String? = null,
        beskrivelse: String? = null,
        mappeId: Long? = null,
        prioritet: OppgavePrioritet = OppgavePrioritet.NORM,
    ): Long {
        val opprettetOppgaveId =
            opprettOppgaveUtenÅLagreIRepository(
                behandlingId = behandlingId,
                oppgavetype = oppgavetype,
                fristFerdigstillelse = null,
                beskrivelse = lagOppgaveTekst(beskrivelse),
                tilordnetNavIdent = tilordnetNavIdent,
                mappeId = mappeId,
                prioritet = prioritet,
            )
        val oppgave = OppgaveDomain(
            gsakOppgaveId = opprettetOppgaveId,
            behandlingId = behandlingId,
            type = oppgavetype,
        )
        oppgaveRepository.insert(oppgave)
        return opprettetOppgaveId
    }

    private fun getOppgaveFinnesFraFør(
        oppgavetype: Oppgavetype,
        behandlingId: UUID,
    ) = oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId, oppgavetype)

    fun oppdaterOppgave(oppgave: Oppgave) {
        oppgaveClient.oppdaterOppgave(oppgave)
    }

    /**
     * I de tilfeller en service ønsker å ansvare selv for lagring til [OppgaveRepository]
     */
    fun opprettOppgaveUtenÅLagreIRepository(
        behandlingId: UUID,
        oppgavetype: Oppgavetype,
        fristFerdigstillelse: LocalDate?,
        beskrivelse: String,
        tilordnetNavIdent: String?,
        mappeId: Long? = null,
        prioritet: OppgavePrioritet = OppgavePrioritet.NORM,
    ): Long {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val personIdent = fagsak.hentAktivIdent()
        val enhetsnummer = arbeidsfordelingService.hentNavEnhetId(personIdent, oppgavetype)
        val opprettOppgave = OpprettOppgaveRequest(
            ident = OppgaveIdentV2(ident = personIdent, gruppe = IdentGruppe.FOLKEREGISTERIDENT),
            tema = Tema.TSO,
            oppgavetype = oppgavetype,
            fristFerdigstillelse = fristFerdigstillelse ?: lagFristForOppgave(LocalDateTime.now()),
            beskrivelse = beskrivelse,
            enhetsnummer = enhetsnummer,
            behandlingstema = finnBehandlingstema(fagsak.stønadstype).value,
            tilordnetRessurs = tilordnetNavIdent,
            mappeId = mappeId,
            prioritet = prioritet,
        )

        return oppgaveClient.opprettOppgave(opprettOppgave)
    }

    fun tilbakestillFordelingPåOppgave(gsakOppgaveId: Long, versjon: Int): Oppgave {
        return oppgaveClient.fordelOppgave(gsakOppgaveId, null, versjon = versjon)
    }

    fun hentOppgaveDomain(oppgaveId: Long): OppgaveDomain? =
        oppgaveRepository.findByGsakOppgaveId(oppgaveId)

    fun hentOppgaveSomIkkeErFerdigstilt(behandlingId: UUID, oppgavetype: Oppgavetype): OppgaveDomain? {
        return oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId, oppgavetype)
    }

    fun hentBehandleSakOppgaveSomIkkeErFerdigstilt(behandlingId: UUID): OppgaveDomain? {
        return oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
            behandlingId,
            setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak),
        )
    }

    fun hentOppgave(gsakOppgaveId: Long): Oppgave {
        return oppgaveClient.finnOppgaveMedId(gsakOppgaveId)
    }

    fun ferdigstillBehandleOppgave(behandlingId: UUID, oppgavetype: Oppgavetype) {
        val oppgave = oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId, oppgavetype)
            ?: error("Finner ikke oppgave for behandling $behandlingId")
        ferdigstillOppgaveOgSettOppgaveDomainTilFerdig(oppgave)
    }

    /**
     * @param ignorerFeilregistrert ignorerer oppgaver som allerede er feilregistrerte
     * Den burde kun settes til true for lukking av oppgaver koblet til henleggelse
     * Oppgaver skal ikke være lukket når denne kalles, då det er ef-sak som burde lukke oppgaver som vi har opprettet
     */
    fun ferdigstillOppgaveHvisOppgaveFinnes(
        behandlingId: UUID,
        oppgavetype: Oppgavetype,
    ) {
        val oppgave = oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId, oppgavetype)
        oppgave?.let {
            ferdigstillOppgaveOgSettOppgaveDomainTilFerdig(oppgave)
        }
    }

    private fun ferdigstillOppgaveOgSettOppgaveDomainTilFerdig(oppgave: OppgaveDomain) {
        ferdigstillOppgave(oppgave.gsakOppgaveId)

        oppgave.erFerdigstilt = true
        oppgaveRepository.update(oppgave)
    }

    fun ferdigstillOppgave(gsakOppgaveId: Long) {
        oppgaveClient.ferdigstillOppgave(gsakOppgaveId)
    }

    fun finnSisteBehandleSakOppgaveForBehandling(behandlingId: UUID): OppgaveDomain? =
        oppgaveRepository.findTopByBehandlingIdAndTypeOrderBySporbarOpprettetTidDesc(
            behandlingId,
            Oppgavetype.BehandleSak,
        )

    fun finnSisteOppgaveForBehandling(behandlingId: UUID): OppgaveDomain? {
        return oppgaveRepository.findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandlingId)
    }

    fun lagOppgaveTekst(beskrivelse: String? = null): String {
        return if (beskrivelse != null) {
            beskrivelse + "\n"
        } else {
            ""
        } +
            "----- Opprettet av tilleggsstonader-sak ${
                LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
            } ---"
    }

    private fun finnBehandlingstema(stønadstype: Stønadstype): Behandlingstema {
        return when (stønadstype) {
            Stønadstype.BARNETILSYN -> Behandlingstema.TilsynBarn
        }
    }

    /**
     * Frist skal være 1 dag hvis den opprettes før kl. 12
     * og 2 dager hvis den opprettes etter kl. 12
     *
     * Helgedager må ekskluderes
     *
     */
    fun lagFristForOppgave(gjeldendeTid: LocalDateTime): LocalDate {
        val frist = when (gjeldendeTid.dayOfWeek) {
            DayOfWeek.FRIDAY -> fristBasertPåKlokkeslett(gjeldendeTid.plusDays(2))
            DayOfWeek.SATURDAY -> fristBasertPåKlokkeslett(gjeldendeTid.plusDays(2).withHour(8))
            DayOfWeek.SUNDAY -> fristBasertPåKlokkeslett(gjeldendeTid.plusDays(1).withHour(8))
            else -> fristBasertPåKlokkeslett(gjeldendeTid)
        }

        return when (frist.dayOfWeek) {
            DayOfWeek.SATURDAY -> frist.plusDays(2)
            DayOfWeek.SUNDAY -> frist.plusDays(1)
            else -> frist
        }
    }

    fun finnMapper(enheter: List<String>): List<MappeDto> {
        return enheter.flatMap { finnMapper(it) }
    }

    fun finnMapper(enhet: String): List<MappeDto> {
        return cacheManager.getValue("oppgave-mappe", enhet) {
            logger.info("Henter mapper på nytt")
            val mappeRespons = oppgaveClient.finnMapper(
                enhetsnummer = enhet,
                limit = 1000,
            )
            if (mappeRespons.antallTreffTotalt > mappeRespons.mapper.size) {
                logger.error(
                    "Det finnes flere mapper (${mappeRespons.antallTreffTotalt}) " +
                        "enn vi har hentet ut (${mappeRespons.mapper.size}). Sjekk limit. ",
                )
            }
            mappeRespons.mapper
        }
    }

    private fun fristBasertPåKlokkeslett(gjeldendeTid: LocalDateTime): LocalDate {
        return if (gjeldendeTid.hour >= 12) {
            return gjeldendeTid.plusDays(2).toLocalDate()
        } else {
            gjeldendeTid.plusDays(1).toLocalDate()
        }
    }

    fun finnBehandleSakOppgaver(
        opprettetTomTidspunktPåBehandleSakOppgave: LocalDateTime,
    ): List<FinnOppgaveResponseDto> {
        val limit: Long = 2000

        val behandleSakOppgaver = oppgaveClient.hentOppgaver(
            finnOppgaveRequest = FinnOppgaveRequest(
                tema = Tema.TSO,
                oppgavetype = Oppgavetype.BehandleSak,
                limit = limit,
                opprettetTomTidspunkt = opprettetTomTidspunktPåBehandleSakOppgave,
            ),
        )

        val behandleUnderkjent = oppgaveClient.hentOppgaver(
            finnOppgaveRequest = FinnOppgaveRequest(
                tema = Tema.TSO,
                oppgavetype = Oppgavetype.BehandleUnderkjentVedtak,
                limit = limit,
            ),
        )

        val godkjenne = oppgaveClient.hentOppgaver(
            finnOppgaveRequest = FinnOppgaveRequest(
                tema = Tema.TSO,
                oppgavetype = Oppgavetype.GodkjenneVedtak,
                limit = limit,
            ),
        )

        logger.info("Hentet oppgaver:  ${behandleSakOppgaver.antallTreffTotalt}, ${behandleUnderkjent.antallTreffTotalt}, ${godkjenne.antallTreffTotalt}")

        feilHvis(behandleSakOppgaver.antallTreffTotalt >= limit) { "For mange behandleSakOppgaver - limit truffet: + $limit " }
        feilHvis(behandleUnderkjent.antallTreffTotalt >= limit) { "For mange behandleUnderkjent - limit truffet: + $limit " }
        feilHvis(godkjenne.antallTreffTotalt >= limit) { "For mange godkjenne - limit truffet: + $limit " }

        return listOf(behandleSakOppgaver, behandleUnderkjent, godkjenne)
    }
}
