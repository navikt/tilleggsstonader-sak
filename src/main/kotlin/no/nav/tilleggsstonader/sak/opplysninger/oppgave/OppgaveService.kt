package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.oppgave.Behandlingstype
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnOppgaveRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.IdentGruppe
import no.nav.tilleggsstonader.kontrakter.oppgave.MappeDto
import no.nav.tilleggsstonader.kontrakter.oppgave.OppdatertOppgaveResponse
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveMappe
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.StatusEnum
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.OppdaterPåVentRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.SettPåVentRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.SettPåVentResponse
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.TaAvVentRequest
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.libs.spring.cache.getCachedOrLoad
import no.nav.tilleggsstonader.libs.spring.cache.getValue
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.klage.KlageService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveUtil.skalPlasseresIKlarMappe
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.domain.FinnOppgaveresultatMedMetadata
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.domain.OppdatertOppgaveHendelse
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.domain.OppgaveMedMetadata
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.domain.OppgaveMetadata
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto.FinnOppgaveRequestDto
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlPersonKort
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gjeldende
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.visningsnavn
import no.nav.tilleggsstonader.sak.util.DatoFormat
import no.nav.tilleggsstonader.sak.util.FnrUtil
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class OppgaveService(
    private val oppgaveClient: OppgaveClient,
    private val fagsakService: FagsakService,
    private val oppgaveRepository: OppgaveRepository,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val cacheManager: CacheManager,
    private val personService: PersonService,
    private val klageService: KlageService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequestDto): FinnOppgaveresultatMedMetadata {
        FnrUtil.validerOptionalIdent(finnOppgaveRequest.ident)

        val aktørId =
            finnOppgaveRequest.ident
                .takeUnless { it.isNullOrBlank() }
                ?.let { personService.hentAktørId(it) }

        val enhet = finnOppgaveRequest.enhet ?: error("Enhet er påkrevd når man søker etter oppgaver")
        val request =
            finnOppgaveRequest.tilFinnOppgaveRequest(
                aktørId,
                klarmappe = finnMappe(enhet, OppgaveMappe.KLAR),
                ventemappe = finnMappe(enhet, OppgaveMappe.PÅ_VENT),
            )
        return finnOppgaver(request)
    }

    fun hentOppgaverForPerson(personIdent: String): FinnOppgaveresultatMedMetadata {
        val aktørId = personService.hentAktørId(personIdent)
        val oppgaveRequest = FinnOppgaveRequest(aktørId = aktørId, tema = listOf(Tema.TSO, Tema.TSR))

        return finnOppgaver(oppgaveRequest)
    }

    private fun finnOppgaver(request: FinnOppgaveRequest): FinnOppgaveresultatMedMetadata {
        val oppgaveResponse = oppgaveClient.hentOppgaver(request)

        val metadata = finnOppgaveMetadata(oppgaveResponse.oppgaver)
        return FinnOppgaveresultatMedMetadata(
            antallTreffTotalt = oppgaveResponse.antallTreffTotalt,
            oppgaver = oppgaveResponse.oppgaver.map { OppgaveMedMetadata(it, metadata[it.id]) },
        )
    }

    private fun finnOppgaveMetadata(oppgave: Oppgave): OppgaveMetadata = finnOppgaveMetadata(listOf(oppgave)).values.single()

    private fun finnOppgaveMetadata(oppgaver: List<Oppgave>): Map<Long, OppgaveMetadata> {
        val personer = personService.hentPersonKortBolk(oppgaver.mapNotNull { it.ident }.distinct())
        val behandlingMetadata = finnOppgaveBehandlingMetadata(oppgaver)
        return oppgaver.associate {
            it.id to
                OppgaveMetadata(
                    navn = personer.visningsnavnFor(it),
                    behandlingMetadata = behandlingMetadata[it.id],
                )
        }
    }

    private fun finnOppgaveBehandlingMetadata(oppgaver: List<Oppgave>): Map<Long, OppgaveBehandlingMetadata> =
        finnOppgaveMetadataSak(oppgaver) + finnOppgaveMetadataKlage(oppgaver)

    private fun finnOppgaveMetadataSak(oppgaver: List<Oppgave>): Map<Long, OppgaveBehandlingMetadata> {
        val oppgaveIder = oppgaver.filter { it.behandlingstype != Behandlingstype.Klage.value }.map { it.id }
        return cacheManager.getCachedOrLoad("oppgaveMetadata", oppgaveIder) {
            oppgaveRepository.finnOppgaveMetadata(oppgaveIder).associateBy { it.gsakOppgaveId }
        }
    }

    private fun finnOppgaveMetadataKlage(oppgaver: List<Oppgave>): Map<Long, OppgaveBehandlingMetadata> {
        val oppgaveIder = oppgaver.filter { it.behandlingstype == Behandlingstype.Klage.value }.map { it.id }
        return cacheManager.getCachedOrLoad("oppgaveMetadataKlage", oppgaveIder) {
            klageService
                .hentBehandlingIderForOppgaveIder(oppgaveIder)
                .map { it.key to OppgaveBehandlingMetadata(gsakOppgaveId = it.key, behandlingId = it.value) }
                .toMap()
        }
    }

    private val Oppgave.ident: String?
        get() = this.identer?.firstOrNull { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }?.ident

    @Transactional
    fun fordelOppgave(
        gsakOppgaveId: Long,
        saksbehandler: String?,
        versjon: Int,
    ): OppgaveMedMetadata {
        val finnOppgave =
            oppgaveRepository.findByGsakOppgaveId(gsakOppgaveId)

        finnOppgave?.let { it ->
            oppgaveRepository.update(it.copy(tilordnetSaksbehandler = saksbehandler))
        }

        val oppdatertOppgave =
            oppgaveClient.fordelOppgave(
                oppgaveId = gsakOppgaveId,
                saksbehandler = saksbehandler,
                versjon = versjon,
            )

        return medMetadata(oppdatertOppgave)
    }

    private fun medMetadata(oppdatertOppgave: Oppgave): OppgaveMedMetadata =
        OppgaveMedMetadata(
            oppgave = oppdatertOppgave,
            metadata = finnOppgaveMetadata(oppdatertOppgave),
        )

    fun opprettOppgave(
        behandlingId: BehandlingId,
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
        behandlingId: BehandlingId?,
        oppgave: OpprettOppgave,
    ): Long {
        feilHvis(oppgave.oppgavetype == Oppgavetype.BehandleSak && behandlingId == null) {
            "Må ha behandlingId når man oppretter oppgave for behandle sak"
        }
        val enhetsnummer = arbeidsfordelingService.hentNavEnhetId(personIdent, stønadstype, oppgave.oppgavetype)
        val mappeId = if (oppgave.skalOpprettesIMappe) utledMappeId(personIdent, oppgave, enhetsnummer) else null
        val opprettetOppgaveId = opprettOppgaveUtenÅLagreIRepository(personIdent, stønadstype, oppgave, enhetsnummer, mappeId)
        val oppgave =
            OppgaveDomain(
                gsakOppgaveId = opprettetOppgaveId,
                behandlingId = behandlingId,
                type = oppgave.oppgavetype,
                tilordnetSaksbehandler = oppgave.tilordnetNavIdent,
                status = Oppgavestatus.ÅPEN,
                tildeltEnhetsnummer = enhetsnummer,
                enhetsmappeId = mappeId,
            )
        oppgaveRepository.insert(oppgave)
        return opprettetOppgaveId
    }

    fun oppdaterOppgave(oppgave: Oppgave): OppdatertOppgaveResponse = oppgaveClient.oppdaterOppgave(oppgave)

    /**
     * I de tilfeller en service ønsker å ansvare selv for lagring til [OppgaveRepository]
     */
    private fun opprettOppgaveUtenÅLagreIRepository(
        personIdent: String,
        stønadstype: Stønadstype,
        oppgave: OpprettOppgave,
        enhetsnummer: String?,
        mappeId: Long? = null,
    ): Long {
        val opprettOppgave =
            tilOpprettOppgaveRequest(
                oppgave = oppgave,
                personIdent = personIdent,
                stønadstype,
                enhetsnummer = enhetsnummer,
                mappeId = mappeId,
            )

        try {
            return oppgaveClient.opprettOppgave(opprettOppgave)
        } catch (e: Exception) {
            secureLogger.warn(
                "Feilet opprettelse av oppgave til tilordnetRessurs=${oppgave.tilordnetNavIdent} " +
                    "enhetsnummer=$enhetsnummer oppgavetype=${oppgave.oppgavetype}",
            )
            throw e
        }
    }

    /**
     * Skal plassere oppgaver vi oppretter som skal håndteres i ny saksbehandling i egen mappe
     * for at de ikke skal blandes med uplasserte som håndteres dagligen i gosys
     */
    private fun utledMappeId(
        ident: String,
        oppgave: OpprettOppgave,
        enhetsnummer: String?,
    ): Long? {
        if (!skalPlasseresIKlarMappe(oppgave.oppgavetype)) {
            return null
        }
        if (enhetsnummer == null) {
            error("Mangler enhetsnummer for oppgave for ident=$ident oppgavetype=${oppgave.oppgavetype}")
        }
        return finnMappe(enhetsnummer, OppgaveMappe.KLAR).id
    }

    fun tilbakestillFordelingPåOppgave(
        gsakOppgaveId: Long,
        versjon: Int,
    ): Oppgave = oppgaveClient.fordelOppgave(gsakOppgaveId, null, versjon = versjon)

    fun hentOppgaveDomainSomIkkeErFerdigstilt(
        behandlingId: BehandlingId,
        oppgavetype: Oppgavetype,
    ): OppgaveDomain? = oppgaveRepository.findByBehandlingIdAndTypeAndStatus(behandlingId, oppgavetype, Oppgavestatus.ÅPEN)

    fun hentAktivBehandleSakOppgave(behandlingId: BehandlingId): Oppgave =
        hentOppgave(hentBehandleSakOppgaveDomainSomIkkeErFerdigstilt(behandlingId).gsakOppgaveId)

    fun hentBehandleSakOppgaveDomainSomIkkeErFerdigstilt(behandlingId: BehandlingId): OppgaveDomain =
        finnBehandleSakOppgaveDomainSomIkkeErFerdigstilt(behandlingId)
            ?: error("Finner ikke aktiv BehandleSak oppgave for behandling $behandlingId")

    fun finnBehandleSakOppgaveDomainSomIkkeErFerdigstilt(behandlingId: BehandlingId): OppgaveDomain? =
        oppgaveRepository.findByBehandlingIdAndStatusAndTypeIn(
            behandlingId = behandlingId,
            status = Oppgavestatus.ÅPEN,
            oppgavetype = setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak),
        )

    fun hentOppgave(gsakOppgaveId: Long): Oppgave = oppgaveClient.finnOppgaveMedId(gsakOppgaveId)

    fun ferdigstillBehandleOppgave(
        behandlingId: BehandlingId,
        oppgavetype: Oppgavetype,
    ) {
        val oppgave =
            oppgaveRepository.findByBehandlingIdAndTypeAndStatus(behandlingId, oppgavetype, Oppgavestatus.ÅPEN)
                ?: error("Finner ikke oppgave for behandling $behandlingId type=$oppgavetype")
        ferdigstillOppgaveOgSettOppgaveDomainTilFerdig(oppgave)
    }

    private fun ferdigstillOppgaveOgSettOppgaveDomainTilFerdig(oppgave: OppgaveDomain) {
        ferdigstillOppgave(oppgave.gsakOppgaveId)

        oppgaveRepository.update(oppgave.copy(status = Oppgavestatus.FERDIGSTILT))
    }

    /**
     *  Forsøker å ferdigstille oppgave hvis den finnes. Hvis oppgaven er feilregistrert i oppgavesystemet vil den bli markert som ferdigstilt.
     */
    fun ferdigstillOppgaveOgsåHvisFeilregistrert(
        behandlingId: BehandlingId,
        oppgavetype: Oppgavetype,
    ) {
        val oppgave = oppgaveRepository.findByBehandlingIdAndTypeAndStatus(behandlingId, oppgavetype, Oppgavestatus.ÅPEN)
        oppgave?.let {
            try {
                ferdigstillOppgaveOgSettOppgaveDomainTilFerdig(oppgave)
            } catch (e: Exception) {
                val oppgaveStatus = hentOppgave(oppgave.gsakOppgaveId).status
                if (oppgaveStatus == StatusEnum.FEILREGISTRERT) {
                    logger.warn("Ferdigstilling - oppgave=${oppgave.id} er feilregistrert. Oppdaterer OppgaveDomain")
                    oppgaveRepository.update(oppgave.copy(status = Oppgavestatus.FEILREGISTRERT))
                } else {
                    throw e
                }
            }
        }
    }

    fun ferdigstillOppgave(gsakOppgaveId: Long) {
        oppgaveClient.ferdigstillOppgave(gsakOppgaveId)
    }

    fun finnSisteOppgaveDomainForBehandling(behandlingId: BehandlingId): OppgaveDomain? =
        oppgaveRepository.findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandlingId)

    fun finnMappe(
        enhet: String,
        oppgaveMappe: OppgaveMappe,
    ) = finnMapper(enhet)
        .let { alleMapper ->
            val aktuelleMapper =
                alleMapper.filter { mappe ->
                    oppgaveMappe.navn.any { mappe.navn.endsWith(it, ignoreCase = true) }
                }
            if (aktuelleMapper.size != 1) {
                secureLogger.error("Finner ${aktuelleMapper.size} mapper for enhet=$enhet navn=$oppgaveMappe - mapper=$alleMapper")
                error("Finner ikke mapper for enhet=$enhet navn=$oppgaveMappe. Se secure logs for mer info")
            }
            aktuelleMapper.single()
        }

    fun finnMapper(enheter: List<String>): List<MappeDto> = enheter.flatMap { finnMapper(it) }

    private fun finnMapper(enhet: String): List<MappeDto> =
        cacheManager.getValue("oppgave-mappe", enhet) {
            logger.info("Henter mapper på nytt")
            val mappeRespons =
                oppgaveClient.finnMapper(
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

    fun settPåVent(settPåVent: SettPåVentRequest): SettPåVentResponse = oppgaveClient.settPåVent(settPåVent)

    fun oppdaterPåVent(oppdaterPåVent: OppdaterPåVentRequest): SettPåVentResponse = oppgaveClient.oppdaterPåVent(oppdaterPåVent)

    fun taAvVent(taAvVent: TaAvVentRequest): SettPåVentResponse = oppgaveClient.taAvVent(taAvVent)

    private fun Map<String, PdlPersonKort>.visningsnavnFor(oppgave: Oppgave) =
        oppgave.ident
            ?.let { this[it] }
            ?.navn
            ?.gjeldende()
            ?.visningsnavn() ?: "Mangler navn"

    fun lagBeskrivelseMelding(
        endring: String,
        nåværendeBeskrivelse: String?,
    ): String {
        val innloggetSaksbehandlerIdent = SikkerhetContext.hentSaksbehandlerEllerSystembruker()
        val saksbehandlerNavn = SikkerhetContext.hentSaksbehandlerNavn(strict = false)
        val formatertDato = LocalDateTime.now().format(DatoFormat.GOSYS_DATE_TIME)

        val prefix = "--- $formatertDato $saksbehandlerNavn ($innloggetSaksbehandlerIdent) ---\n"

        return prefix + endring + nåværendeBeskrivelse?.let { "\n\n$it" }.orEmpty()
    }

    fun håndterOppdatertOppgaveHendelse(oppdatertOppgaveHendelse: OppdatertOppgaveHendelse) {
        oppgaveRepository.findByGsakOppgaveId(oppdatertOppgaveHendelse.gsakOppgaveId)?.let { oppgave ->
            if (!oppgave.erIgnorert()) { // Om satt til ignorert ønsker vi ikke oppdateringer
                oppgaveRepository.update(
                    oppgave.copy(
                        tilordnetSaksbehandler = oppdatertOppgaveHendelse.tilordnetSaksbehandler,
                        status = oppdatertOppgaveHendelse.status,
                        tildeltEnhetsnummer = oppdatertOppgaveHendelse.tildeltEnhetsnummer,
                        enhetsmappeId = oppdatertOppgaveHendelse.enhetsmappeId,
                    ),
                )
                logger.info("Oppdatert oppgave med gsakOppgaveId ${oppdatertOppgaveHendelse.gsakOppgaveId}")
            }
        }
    }
}
