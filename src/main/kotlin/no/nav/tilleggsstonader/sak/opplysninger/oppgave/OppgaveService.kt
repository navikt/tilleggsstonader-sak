package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.tilleggsstonader.kontrakter.felles.Behandlingstema
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnOppgaveRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.IdentGruppe
import no.nav.tilleggsstonader.kontrakter.oppgave.MappeDto
import no.nav.tilleggsstonader.kontrakter.oppgave.OppdatertOppgaveResponse
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveIdentV2
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.config.getCachedOrLoad
import no.nav.tilleggsstonader.sak.infrastruktur.config.getValue
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveUtil.skalPlasseresIKlarMappe
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveUtil.utledBehandlesAvApplikasjon
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto.FinnOppgaveRequestDto
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto.FinnOppgaveResponseDto
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto.OppgaveDto
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlPersonKort
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gjeldende
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.visningsnavn
import no.nav.tilleggsstonader.sak.util.FnrUtil
import no.nav.tilleggsstonader.sak.util.medGosysTid
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class OppgaveService(
    private val oppgaveClient: OppgaveClient,
    private val fagsakService: FagsakService,
    private val oppgaveRepository: OppgaveRepository,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val cacheManager: CacheManager,
    private val personService: PersonService,
    private val unleashService: UnleashService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequestDto): FinnOppgaveResponseDto {
        FnrUtil.validerOptionalIdent(finnOppgaveRequest.ident)

        val aktørId = finnOppgaveRequest.ident.takeUnless { it.isNullOrBlank() }
            ?.let { personService.hentAktørId(it) }

        val request = finnOppgaveRequest.tilFinnOppgaveRequest(aktørId, finnVentemappe())
        return finnOppgaver(request)
    }

    fun hentOppgaverForPerson(personIdent: String): FinnOppgaveResponseDto {
        val aktørId = personService.hentAktørId(personIdent)
        val oppgaveRequest = FinnOppgaveRequest(aktørId = aktørId, tema = Tema.TSO)

        return finnOppgaver(oppgaveRequest)
    }

    private fun finnOppgaver(request: FinnOppgaveRequest): FinnOppgaveResponseDto {
        val oppgaveResponse = oppgaveClient.hentOppgaver(request)

        val personer = personService.hentPersonKortBolk(oppgaveResponse.oppgaver.mapNotNull { it.ident }.distinct())
        val oppgaveMetadata = finnOppgaveMetadata(oppgaveResponse.oppgaver)

        return FinnOppgaveResponseDto(
            antallTreffTotalt = oppgaveResponse.antallTreffTotalt,
            oppgaver = oppgaveResponse.oppgaver.map { oppgave ->
                OppgaveDto(
                    oppgave = oppgave,
                    navn = personer.visningsnavnFor(oppgave),
                    oppgaveMetadata = oppgaveMetadata[oppgave.id],
                )
            },
        )
    }

    private fun finnOppgaveMetadata(oppgaver: List<Oppgave>): Map<Long, OppgaveMetadata> {
        val oppgaveIder = oppgaver.map { it.id }
        return cacheManager.getCachedOrLoad("oppgaveMetadata", oppgaveIder) {
            oppgaveRepository.finnOppgaveMetadata(oppgaveIder).associateBy { it.gsakOppgaveId }
        }
    }

    private val Oppgave.ident: String?
        get() = this.identer?.firstOrNull { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }?.ident

    fun fordelOppgave(gsakOppgaveId: Long, saksbehandler: String?, versjon: Int): OppgaveDto {
        val oppdatertOppgave = oppgaveClient.fordelOppgave(
            oppgaveId = gsakOppgaveId,
            saksbehandler = saksbehandler,
            versjon = versjon,
        )
        val personer = personService.hentPersonKortBolk(listOfNotNull(oppdatertOppgave.ident))
        return OppgaveDto(
            oppgave = oppdatertOppgave,
            navn = personer.visningsnavnFor(oppdatertOppgave),
            oppgaveMetadata = finnOppgaveMetadata(listOf(oppdatertOppgave))[oppdatertOppgave.id],
        )
    }

    fun opprettOppgave(
        behandlingId: UUID,
        oppgave: OpprettOppgave,
    ): Long {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        return opprettOppgave(
            personIdent = fagsak.hentAktivIdent(),
            stønadstype = fagsak.stønadstype,
            behandlingId = behandlingId,
            oppgave = oppgave,
        )
    }

    fun opprettOppgave(
        personIdent: String,
        stønadstype: Stønadstype,
        behandlingId: UUID?,
        oppgave: OpprettOppgave,
    ): Long {
        feilHvis(oppgave.oppgavetype == Oppgavetype.BehandleSak && behandlingId == null) {
            "Må ha behandlingId når man oppretter oppgave for behandle sak"
        }
        val opprettetOppgaveId = opprettOppgaveUtenÅLagreIRepository(personIdent, stønadstype, oppgave)
        val oppgave = OppgaveDomain(
            gsakOppgaveId = opprettetOppgaveId,
            behandlingId = behandlingId,
            type = oppgave.oppgavetype,
        )
        oppgaveRepository.insert(oppgave)
        return opprettetOppgaveId
    }

    private fun getOppgaveFinnesFraFør(
        oppgavetype: Oppgavetype,
        behandlingId: UUID,
    ) = oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId, oppgavetype)

    fun oppdaterOppgave(oppgave: Oppgave): OppdatertOppgaveResponse {
        return oppgaveClient.oppdaterOppgave(oppgave)
    }

    /**
     * I de tilfeller en service ønsker å ansvare selv for lagring til [OppgaveRepository]
     */
    private fun opprettOppgaveUtenÅLagreIRepository(
        personIdent: String,
        stønadstype: Stønadstype,
        oppgave: OpprettOppgave,
    ): Long {
        val enhetsnummer =
            oppgave.enhetsnummer ?: arbeidsfordelingService.hentNavEnhetId(personIdent, oppgave.oppgavetype)

        val opprettOppgave = OpprettOppgaveRequest(
            ident = OppgaveIdentV2(ident = personIdent, gruppe = IdentGruppe.FOLKEREGISTERIDENT),
            tema = Tema.TSO,
            journalpostId = oppgave.journalpostId,
            oppgavetype = oppgave.oppgavetype,
            fristFerdigstillelse = oppgave.fristFerdigstillelse ?: lagFristForOppgave(osloNow()),
            beskrivelse = lagOppgaveTekst(oppgave.beskrivelse),
            enhetsnummer = enhetsnummer,
            behandlingstema = finnBehandlingstema(stønadstype).value,
            tilordnetRessurs = oppgave.tilordnetNavIdent,
            mappeId = utledMappeId(personIdent, oppgave, enhetsnummer),
            prioritet = oppgave.prioritet,
            behandlesAvApplikasjon = utledBehandlesAvApplikasjon(oppgave.oppgavetype),
        )

        return oppgaveClient.opprettOppgave(opprettOppgave)
    }

    /**
     * Skal plassere oppgaver vi oppretter som skal håndteres i ny saksbehandling i egen mappe
     * for at de ikke skal blandes med uplasserte som håndteres dagligen i gosys
     */
    private fun utledMappeId(ident: String, oppgave: OpprettOppgave, enhetsnummer: String?): Long? {
        if (!unleashService.isEnabled(Toggle.OPPGAVE_BRUK_KLAR_MAPPE)) {
            return null
        }
        if (!skalPlasseresIKlarMappe(oppgave.oppgavetype)) {
            return null
        }
        if (enhetsnummer == null) {
            error("Mangler enhetsnummer for oppgave for ident=$ident oppgavetype=$oppgave.oppgavetype")
        }
        return finnMappe(enhetsnummer, OppgaveMappe.KLAR)
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
            ?: error("Finner ikke oppgave for behandling $behandlingId type=$oppgavetype")
        ferdigstillOppgaveOgSettOppgaveDomainTilFerdig(oppgave)
    }

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

    private fun lagOppgaveTekst(beskrivelse: String? = null): String {
        val tidspunkt = osloNow().medGosysTid()
        val prefix = "----- Opprettet av tilleggsstonader-sak $tidspunkt ---"
        val beskrivelseMedNewLine = beskrivelse?.let { "\n$it" } ?: ""
        return prefix + beskrivelseMedNewLine
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

    // TODO: Bruk enhet fra saksbehandler
    // TODO: Bruk finnMappe når På-vent-mappen endret navn
    fun finnVentemappe(): MappeDto {
        val mapper = finnMapper("4462")
        return mapper.singleOrNull { it.navn == OppgaveMappe.PÅ_VENT.navn }
            ?: mapper.single { it.navn == "10 På vent" }
    }

    fun finnMappe(enhet: String, oppgaveMappe: OppgaveMappe) = finnMapper(enhet)
        .filter { it.navn.endsWith(oppgaveMappe.navn, ignoreCase = true) }
        .let {
            if (it.size != 1) {
                secureLogger.error("Finner ${it.size} mapper for enhet=$enhet navn=$oppgaveMappe - mapper=$it")
                error("Finner ikke mapper for enhet=$enhet navn=$oppgaveMappe. Se secure logs for mer info")
            }
            it.single()
        }
        .id

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

    private fun Map<String, PdlPersonKort>.visningsnavnFor(oppgave: Oppgave) =
        oppgave.ident?.let { this[it] }?.navn?.gjeldende()?.visningsnavn() ?: "Mangler navn"
}
