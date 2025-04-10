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
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.config.getCachedOrLoad
import no.nav.tilleggsstonader.sak.infrastruktur.config.getValue
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.klage.KlageService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveUtil.skalPlasseresIKlarMappe
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.domain.FinnOppgaveresultatMedMetadata
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.domain.OppgaveMedMetadata
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.domain.OppgaveMetadata
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto.FinnOppgaveRequestDto
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlPersonKort
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gjeldende
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.visningsnavn
import no.nav.tilleggsstonader.sak.util.FnrUtil
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service

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
        val oppgaveRequest = FinnOppgaveRequest(aktørId = aktørId, tema = Tema.TSO)

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

    fun fordelOppgave(
        gsakOppgaveId: Long,
        saksbehandler: String?,
        versjon: Int,
    ): OppgaveMedMetadata {
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
        val opprettetOppgaveId = opprettOppgaveUtenÅLagreIRepository(personIdent, stønadstype, oppgave)
        val oppgave =
            OppgaveDomain(
                gsakOppgaveId = opprettetOppgaveId,
                behandlingId = behandlingId,
                type = oppgave.oppgavetype,
            )
        oppgaveRepository.insert(oppgave)
        return opprettetOppgaveId
    }

    private fun getOppgaveFinnesFraFør(
        oppgavetype: Oppgavetype,
        behandlingId: BehandlingId,
    ) = oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId, oppgavetype)

    fun oppdaterOppgave(oppgave: Oppgave): OppdatertOppgaveResponse = oppgaveClient.oppdaterOppgave(oppgave)

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

        val mappeId = utledMappeId(personIdent, oppgave, enhetsnummer)
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

    fun hentOppgaveDomain(oppgaveId: Long): OppgaveDomain? = oppgaveRepository.findByGsakOppgaveId(oppgaveId)

    fun hentOppgaveSomIkkeErFerdigstilt(
        behandlingId: BehandlingId,
        oppgavetype: Oppgavetype,
    ): OppgaveDomain? = oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId, oppgavetype)

    fun hentBehandleSakOppgaveSomIkkeErFerdigstilt(behandlingId: BehandlingId): OppgaveDomain? =
        oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
            behandlingId,
            setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak),
        )

    fun hentOppgave(gsakOppgaveId: Long): Oppgave = oppgaveClient.finnOppgaveMedId(gsakOppgaveId)

    fun ferdigstillBehandleOppgave(
        behandlingId: BehandlingId,
        oppgavetype: Oppgavetype,
    ) {
        val oppgave =
            oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId, oppgavetype)
                ?: error("Finner ikke oppgave for behandling $behandlingId type=$oppgavetype")
        ferdigstillOppgaveOgSettOppgaveDomainTilFerdig(oppgave)
    }

    private fun ferdigstillOppgaveOgSettOppgaveDomainTilFerdig(oppgave: OppgaveDomain) {
        ferdigstillOppgave(oppgave.gsakOppgaveId)

        oppgave.erFerdigstilt = true
        oppgaveRepository.update(oppgave)
    }

    /**
     *  Forsøker å ferdigstille oppgave hvis den finnes. Hvis oppgaven er feilregistrert i oppgavesystemet vil den bli markert som ferdigstilt.
     */
    fun ferdigstillOppgaveOgsåHvisFeilregistrert(
        behandlingId: BehandlingId,
        oppgavetype: Oppgavetype,
    ) {
        val oppgave = oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId, oppgavetype)
        oppgave?.let {
            try {
                ferdigstillOppgaveOgSettOppgaveDomainTilFerdig(oppgave)
            } catch (e: Exception) {
                val oppgaveStatus = hentOppgave(oppgave.gsakOppgaveId).status
                if (oppgaveStatus == StatusEnum.FEILREGISTRERT) {
                    logger.warn("Ferdigstilling - oppgave=${oppgave.id} er feilregistrert. Markerer oppgave som ferdigstilt")
                    oppgaveRepository.update(oppgave.copy(erFerdigstilt = true))
                } else {
                    throw e
                }
            }
        }
    }

    fun ferdigstillOppgave(gsakOppgaveId: Long) {
        oppgaveClient.ferdigstillOppgave(gsakOppgaveId)
    }

    fun finnSisteBehandleSakOppgaveForBehandling(behandlingId: BehandlingId): OppgaveDomain? =
        oppgaveRepository.findTopByBehandlingIdAndTypeOrderBySporbarOpprettetTidDesc(
            behandlingId,
            Oppgavetype.BehandleSak,
        )

    fun finnSisteOppgaveForBehandling(behandlingId: BehandlingId): OppgaveDomain? =
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

    fun finnMapper(enhet: String): List<MappeDto> =
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

    private fun Map<String, PdlPersonKort>.visningsnavnFor(oppgave: Oppgave) =
        oppgave.ident
            ?.let { this[it] }
            ?.navn
            ?.gjeldende()
            ?.visningsnavn() ?: "Mangler navn"
}
