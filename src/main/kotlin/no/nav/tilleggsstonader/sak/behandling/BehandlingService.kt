package no.nav.tilleggsstonader.sak.behandling

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.sortertEtterVedtakstidspunkt
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.sortertEtterVedtakstidspunktEllerEndretTid
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.utledBehandlingType
import no.nav.tilleggsstonader.sak.behandling.OpprettBehandlingUtil.validerKanOppretteNyBehandling
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingKategori
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus.FERDIGSTILT
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus.SATT_PÅ_VENT
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus.UTREDES
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Behandlingsjournalpost
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingsjournalpostRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingId
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingIdRepository
import no.nav.tilleggsstonader.sak.behandling.domain.Journalposttype
import no.nav.tilleggsstonader.sak.behandling.domain.NyeOpplysningerMetadata
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandling.dto.HenlagtDto
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.Behandlingshistorikk
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findAllByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.statistikk.task.BehandlingsstatistikkTask
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class BehandlingService(
    private val behandlingsjournalpostRepository: BehandlingsjournalpostRepository,
    private val behandlingRepository: BehandlingRepository,
    private val eksternBehandlingIdRepository: EksternBehandlingIdRepository,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val taskService: TaskService,
    private val unleashService: UnleashService,
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun hentAktivIdent(behandlingId: BehandlingId): String = behandlingRepository.finnAktivIdent(behandlingId)

    fun hentEksterneIder(behandlingIder: Set<BehandlingId>) =
        behandlingIder
            .takeIf { it.isNotEmpty() }
            ?.let { behandlingRepository.finnEksterneIder(it) } ?: emptySet()

    fun finnSisteIverksatteBehandling(fagsakId: FagsakId) = behandlingRepository.finnSisteIverksatteBehandling(fagsakId)

    fun finnesIkkeFerdigstiltBehandling(fagsakId: FagsakId) = behandlingRepository.existsByFagsakIdAndStatusIsNot(fagsakId, FERDIGSTILT)

    fun finnesBehandlingSomIkkeErFerdigstiltEllerSattPåVent(fagsakId: FagsakId) =
        behandlingRepository.existsByFagsakIdAndStatusIsNotIn(fagsakId, listOf(FERDIGSTILT, SATT_PÅ_VENT))

    fun finnSisteIverksatteBehandlingMedEventuellAvslått(fagsakId: FagsakId): Behandling? =
        behandlingRepository.finnSisteIverksatteBehandling(fagsakId)
            ?: hentBehandlinger(fagsakId).lastOrNull {
                it.status == FERDIGSTILT && it.resultat != BehandlingResultat.HENLAGT
            }

    fun hentBehandlingsjournalposter(behandlingId: BehandlingId): List<Behandlingsjournalpost> =
        behandlingsjournalpostRepository.findAllByBehandlingId(behandlingId)

    fun hentBehandlingerForGjenbrukAvVilkår(fagsakPersonId: FagsakPersonId): List<Behandling> =
        behandlingRepository
            .finnBehandlingerForGjenbrukAvVilkår(fagsakPersonId)
            .sortertEtterVedtakstidspunktEllerEndretTid()
            .reversed()

    @Transactional
    fun opprettBehandling(
        fagsakId: FagsakId,
        status: BehandlingStatus = BehandlingStatus.OPPRETTET,
        stegType: StegType = StegType.INNGANGSVILKÅR,
        behandlingsårsak: BehandlingÅrsak,
        kravMottatt: LocalDate? = null,
        nyeOpplysningerMetadata: NyeOpplysningerMetadata? = null,
    ): Behandling {
        brukerfeilHvis(kravMottatt != null && kravMottatt.isAfter(osloDateNow())) {
            "Kan ikke sette krav mottattdato frem i tid"
        }
        feilHvisIkke(unleashService.isEnabled(Toggle.KAN_OPPRETTE_BEHANDLING)) {
            "Feature toggle for å opprette behandling er slått av"
        }

        val tidligereBehandlinger = behandlingRepository.findByFagsakId(fagsakId)
        val forrigeBehandling = behandlingRepository.finnSisteIverksatteBehandling(fagsakId)
        val behandlingType = utledBehandlingType(tidligereBehandlinger)

        validerKanOppretteNyBehandling(behandlingType, tidligereBehandlinger)

        val behandling =
            behandlingRepository.insert(
                Behandling(
                    fagsakId = fagsakId,
                    forrigeIverksatteBehandlingId = forrigeBehandling?.id,
                    type = behandlingType,
                    steg = stegType,
                    status = status,
                    resultat = BehandlingResultat.IKKE_SATT,
                    årsak = behandlingsårsak,
                    kravMottatt = kravMottatt,
                    kategori = BehandlingKategori.NASJONAL,
                    nyeOpplysningerMetadata = nyeOpplysningerMetadata,
                ),
            )
        eksternBehandlingIdRepository.insert(EksternBehandlingId(behandlingId = behandling.id))

        behandlingshistorikkService.opprettHistorikkInnslag(
            behandlingshistorikk =
                Behandlingshistorikk(
                    behandlingId = behandling.id,
                    steg = stegType,
                    gitVersjon = Applikasjonsversjon.versjon,
                ),
        )

        return behandling
    }

    fun hentBehandling(behandlingId: BehandlingId): Behandling = behandlingRepository.findByIdOrThrow(behandlingId)

    fun hentSaksbehandling(behandlingId: BehandlingId): Saksbehandling = behandlingRepository.finnSaksbehandling(behandlingId)

    fun hentSaksbehandling(eksternBehandlingId: Long): Saksbehandling = behandlingRepository.finnSaksbehandling(eksternBehandlingId)

    fun hentEksternBehandlingId(behandlingId: BehandlingId) = eksternBehandlingIdRepository.findByBehandlingId(behandlingId)

    fun hentBehandlingPåEksternId(eksternBehandlingId: Long): Behandling =
        behandlingRepository.finnMedEksternId(
            eksternBehandlingId,
        ) ?: error("Kan ikke finne behandling med eksternId=$eksternBehandlingId")

    fun hentBehandlinger(behandlingIder: Set<BehandlingId>): List<Behandling> =
        behandlingRepository.findAllByIdOrThrow(behandlingIder) { it.id }

    fun oppdaterStatusPåBehandling(
        behandlingId: BehandlingId,
        status: BehandlingStatus,
    ): Behandling {
        val behandling = hentBehandling(behandlingId)
        secureLogger.info(
            "${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} endrer status på behandling $behandlingId " +
                "fra ${behandling.status} til $status",
        )

        if (BehandlingStatus.UTREDES == status) {
            taskService.save(BehandlingsstatistikkTask.opprettPåbegyntTask(behandlingId))
        }

        return behandlingRepository.update(behandling.copy(status = status))
    }

    // TODO skal vi sette kategori på behandling?
    fun oppdaterKategoriPåBehandling(
        behandlingId: BehandlingId,
        kategori: BehandlingKategori,
    ): Behandling {
        val behandling = hentBehandling(behandlingId)
        secureLogger.info(
            "${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} endrer kategori på behandling $behandlingId " +
                "fra ${behandling.kategori} til $kategori",
        )
        return behandlingRepository.update(behandling.copy(kategori = kategori))
    }

    fun oppdaterforrigeIverksatteBehandlingId(
        behandlingId: BehandlingId,
        forrigeIverksatteBehandlingId: BehandlingId,
    ): Behandling {
        val behandling = hentBehandling(behandlingId)
        behandling.status.validerKanBehandlingRedigeres()
        secureLogger.info(
            "${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} endrer forrigeIverksatteBehandlingId på behandling $behandlingId " +
                "fra ${behandling.forrigeIverksatteBehandlingId} til $forrigeIverksatteBehandlingId",
        )
        return behandlingRepository.update(behandling.copy(forrigeIverksatteBehandlingId = forrigeIverksatteBehandlingId))
    }

    fun oppdaterStegPåBehandling(
        behandlingId: BehandlingId,
        steg: StegType,
    ): Behandling {
        val behandling = hentBehandling(behandlingId)
        secureLogger.info(
            "${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} endrer steg på behandling $behandlingId " +
                "fra ${behandling.steg} til $steg",
        )
        return behandlingRepository.update(behandling.copy(steg = steg))
    }

    fun oppdaterKravMottatt(
        behandlingId: BehandlingId,
        kravMottatt: LocalDate?,
    ): Behandling = behandlingRepository.update(hentBehandling(behandlingId).copy(kravMottatt = kravMottatt))

    fun finnesBehandlingForFagsak(fagsakId: FagsakId) = behandlingRepository.existsByFagsakId(fagsakId)

    fun hentBehandlinger(fagsakId: FagsakId): List<Behandling> =
        behandlingRepository.findByFagsakId(fagsakId).sortertEtterVedtakstidspunkt()

    fun leggTilBehandlingsjournalpost(
        journalpostId: String,
        journalposttype: Journalposttype,
        behandlingId: BehandlingId,
    ) {
        behandlingsjournalpostRepository.insert(
            Behandlingsjournalpost(
                behandlingId = behandlingId,
                journalpostId = journalpostId,
                sporbar = Sporbar(),
                journalpostType = journalposttype,
            ),
        )
    }

    @Transactional
    fun henleggBehandling(
        behandlingId: BehandlingId,
        henlagt: HenlagtDto,
    ): Behandling {
        val behandling = hentBehandling(behandlingId)
        validerAtBehandlingenKanHenlegges(behandling)
        val henlagtBehandling =
            behandling.copy(
                henlagtÅrsak = henlagt.årsak,
                henlagtBegrunnelse = henlagt.begrunnelse,
                resultat = BehandlingResultat.HENLAGT,
                steg = StegType.BEHANDLING_FERDIGSTILT,
                status = FERDIGSTILT,
                vedtakstidspunkt = SporbarUtils.now(),
            )
        behandlingshistorikkService.opprettHistorikkInnslag(
            behandlingId = henlagtBehandling.id,
            stegtype = henlagtBehandling.steg,
            utfall = StegUtfall.HENLAGT,
            metadata = henlagt,
        )

        fjernFritekstFraBehandlingshistorikk(behandlingId)

        taskService.save(
            BehandlingsstatistikkTask.opprettFerdigTask(
                behandlingId = henlagtBehandling.id,
            ),
        )
        return behandlingRepository.update(henlagtBehandling)
    }

    private fun validerAtBehandlingenKanHenlegges(behandling: Behandling) {
        if (!behandling.kanHenlegges()) {
            throw ApiFeil(
                "Kan ikke henlegge en behandling med status ${behandling.status} for ${behandling.type}",
                HttpStatus.BAD_REQUEST,
            )
        }
    }

    /**
     * Setter endelig resultat på behandling, setter vedtakstidspunkt på behandling
     */
    fun oppdaterResultatPåBehandling(
        behandlingId: BehandlingId,
        behandlingResultat: BehandlingResultat,
    ): Behandling {
        val behandling = hentBehandling(behandlingId)
        feilHvis(behandlingResultat == BehandlingResultat.IKKE_SATT) {
            "Må sette et endelig resultat og ikke $behandlingResultat"
        }
        feilHvis(behandling.resultat != BehandlingResultat.IKKE_SATT) {
            "Kan ikke endre resultat på behandling når resultat=${behandling.resultat}"
        }
        return behandlingRepository.update(
            behandling.copy(
                resultat = behandlingResultat,
                vedtakstidspunkt = SporbarUtils.now(),
            ),
        )
    }

    fun utledNesteBehandlingstype(fagsakId: FagsakId): BehandlingType {
        val behandlinger = hentBehandlinger(fagsakId)
        return utledBehandlingType(behandlinger)
    }

    /**
     * Brukes i de tilfellene man ikke har generellt behov for behandling, men ønsker kun å sjekke om den er låst
     */
    fun behandlingErLåstForVidereRedigering(behandlingId: BehandlingId): Boolean =
        behandlingRepository.findByIdOrThrow(behandlingId).status.behandlingErLåstForVidereRedigering()

    fun fjernFritekstFraBehandlingshistorikk(behandlingId: BehandlingId) {
        behandlingshistorikkService.slettFritekstMetadataVedFerdigstillelse(behandlingId)
    }

    fun markerBehandlingSomPåbegynt(
        behandlingId: BehandlingId,
        behandlingStatus: BehandlingStatus,
        behandlingSteg: StegType,
    ) {
        if (behandlingStatus !== UTREDES && behandlingSteg == StegType.INNGANGSVILKÅR) {
            oppdaterStatusPåBehandling(behandlingId, UTREDES)
            behandlingshistorikkService.opprettHistorikkInnslag(
                behandlingId = behandlingId,
                stegtype = StegType.INNGANGSVILKÅR,
                utfall = StegUtfall.UTREDNING_PÅBEGYNT,
                metadata = null,
            )
        }
    }
}
