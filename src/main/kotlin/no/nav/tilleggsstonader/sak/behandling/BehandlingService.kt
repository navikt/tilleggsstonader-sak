package no.nav.tilleggsstonader.sak.behandling

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.sortertEtterVedtakstidspunkt
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.sortertEtterVedtakstidspunktEllerEndretTid
import no.nav.tilleggsstonader.sak.behandling.OpprettBehandlingUtil.validerKanOppretteNyBehandling
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingKategori
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus.FERDIGSTILT
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus.SATT_PÅ_VENT
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Behandlingsjournalpost
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingsjournalpostRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingId
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingIdRepository
import no.nav.tilleggsstonader.sak.behandling.domain.Journalposttype
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandling.dto.HenlagtDto
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.Behandlingshistorikk
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
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
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

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

    fun hentAktivIdent(behandlingId: UUID): String = behandlingRepository.finnAktivIdent(behandlingId)

    fun hentEksterneIder(behandlingIder: Set<UUID>) = behandlingIder.takeIf { it.isNotEmpty() }
        ?.let { behandlingRepository.finnEksterneIder(it) } ?: emptySet()

    fun finnSisteIverksatteBehandling(fagsakId: UUID) =
        behandlingRepository.finnSisteIverksatteBehandling(fagsakId)

    fun hentUferdigeBehandlingerOpprettetFørDato(
        stønadtype: Stønadstype,
        opprettetFørDato: LocalDateTime = LocalDateTime.now().minusMonths(1),
    ): List<Behandling> {
        return behandlingRepository.hentUferdigeBehandlingerOpprettetFørDato(stønadtype, opprettetFørDato)
    }

    fun finnesÅpenBehandling(fagsakId: UUID) =
        behandlingRepository.existsByFagsakIdAndStatusIsNot(fagsakId, FERDIGSTILT)

    fun finnesBehandlingSomIkkeErFerdigstiltEllerSattPåVent(fagsakId: UUID) =
        behandlingRepository.existsByFagsakIdAndStatusIsNotIn(fagsakId, listOf(FERDIGSTILT, SATT_PÅ_VENT))

    fun finnSisteIverksatteBehandlingMedEventuellAvslått(fagsakId: UUID): Behandling? =
        behandlingRepository.finnSisteIverksatteBehandling(fagsakId)
            ?: hentBehandlinger(fagsakId).lastOrNull {
                it.status == FERDIGSTILT && it.resultat != BehandlingResultat.HENLAGT
            }

    fun hentBehandlingsjournalposter(behandlingId: UUID): List<Behandlingsjournalpost> {
        return behandlingsjournalpostRepository.findAllByBehandlingId(behandlingId)
    }

    fun hentBehandlingerForGjenbrukAvVilkår(fagsakPersonId: UUID): List<Behandling> {
        return behandlingRepository.finnBehandlingerForGjenbrukAvVilkår(fagsakPersonId)
            .sortertEtterVedtakstidspunktEllerEndretTid()
            .reversed()
    }

    @Transactional
    fun opprettBehandling(
        behandlingType: BehandlingType,
        fagsakId: UUID,
        status: BehandlingStatus = BehandlingStatus.OPPRETTET,
        stegType: StegType = StegType.INNGANGSVILKÅR,
        behandlingsårsak: BehandlingÅrsak,
        kravMottatt: LocalDate? = null,
        erMigrering: Boolean = false,
    ): Behandling {
        brukerfeilHvis(kravMottatt != null && kravMottatt.isAfter(LocalDate.now())) {
            "Kan ikke sette krav mottattdato frem i tid"
        }
        feilHvisIkke(unleashService.isEnabled(Toggle.KAN_OPPRETTE_BEHANDLING)) {
            "Feature toggle for å opprette behandling er slått av"
        }

        val tidligereBehandlinger = behandlingRepository.findByFagsakId(fagsakId)
        val forrigeBehandling = behandlingRepository.finnSisteIverksatteBehandling(fagsakId)
        validerKanOppretteNyBehandling(behandlingType, tidligereBehandlinger, erMigrering)

        val behandling = behandlingRepository.insert(
            Behandling(
                fagsakId = fagsakId,
                forrigeBehandlingId = forrigeBehandling?.id,
                type = behandlingType,
                steg = stegType,
                status = status,
                resultat = BehandlingResultat.IKKE_SATT,
                årsak = behandlingsårsak,
                kravMottatt = kravMottatt,
                kategori = BehandlingKategori.NASJONAL,
            ),
        )
        eksternBehandlingIdRepository.insert(EksternBehandlingId(behandlingId = behandling.id))

        behandlingshistorikkService.opprettHistorikkInnslag(
            behandlingshistorikk = Behandlingshistorikk(
                behandlingId = behandling.id,
                steg = stegType,
            ),
        )

        return behandling
    }

    fun hentBehandling(behandlingId: UUID): Behandling = behandlingRepository.findByIdOrThrow(behandlingId)

    fun hentSaksbehandling(behandlingId: UUID): Saksbehandling = behandlingRepository.finnSaksbehandling(behandlingId)

    fun hentSaksbehandling(eksternBehandlingId: Long): Saksbehandling =
        behandlingRepository.finnSaksbehandling(eksternBehandlingId)

    fun hentEksternBehandlingId(behandlingId: UUID) = eksternBehandlingIdRepository.findByBehandlingId(behandlingId)

    fun hentBehandlingPåEksternId(eksternBehandlingId: Long): Behandling = behandlingRepository.finnMedEksternId(
        eksternBehandlingId,
    ) ?: error("Kan ikke finne behandling med eksternId=$eksternBehandlingId")

    fun hentBehandlinger(behandlingIder: Set<UUID>): List<Behandling> =
        behandlingRepository.findAllByIdOrThrow(behandlingIder) { it.id }

    fun oppdaterStatusPåBehandling(behandlingId: UUID, status: BehandlingStatus): Behandling {
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
    fun oppdaterKategoriPåBehandling(behandlingId: UUID, kategori: BehandlingKategori): Behandling {
        val behandling = hentBehandling(behandlingId)
        secureLogger.info(
            "${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} endrer kategori på behandling $behandlingId " +
                "fra ${behandling.kategori} til $kategori",
        )
        return behandlingRepository.update(behandling.copy(kategori = kategori))
    }

    fun oppdaterForrigeBehandlingId(behandlingId: UUID, forrigeBehandlingId: UUID): Behandling {
        val behandling = hentBehandling(behandlingId)
        feilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke endre forrigeBehandlingId når behandlingen er låst"
        }
        secureLogger.info(
            "${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} endrer forrigeBehandlingId på behandling $behandlingId " +
                "fra ${behandling.forrigeBehandlingId} til $forrigeBehandlingId",
        )
        return behandlingRepository.update(behandling.copy(forrigeBehandlingId = forrigeBehandlingId))
    }

    fun oppdaterStegPåBehandling(behandlingId: UUID, steg: StegType): Behandling {
        val behandling = hentBehandling(behandlingId)
        secureLogger.info(
            "${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} endrer steg på behandling $behandlingId " +
                "fra ${behandling.steg} til $steg",
        )
        return behandlingRepository.update(behandling.copy(steg = steg))
    }

    fun oppdaterKravMottatt(behandlingId: UUID, kravMottatt: LocalDate?): Behandling {
        return behandlingRepository.update(hentBehandling(behandlingId).copy(kravMottatt = kravMottatt))
    }

    fun finnesBehandlingForFagsak(fagsakId: UUID) =
        behandlingRepository.existsByFagsakId(fagsakId)

    fun hentBehandlinger(fagsakId: UUID): List<Behandling> {
        return behandlingRepository.findByFagsakId(fagsakId).sortertEtterVedtakstidspunkt()
    }

    fun leggTilBehandlingsjournalpost(journalpostId: String, journalposttype: Journalposttype, behandlingId: UUID) {
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
    fun henleggBehandling(behandlingId: UUID, henlagt: HenlagtDto): Behandling {
        val behandling = hentBehandling(behandlingId)
        validerAtBehandlingenKanHenlegges(behandling)
        val henlagtBehandling = behandling.copy(
            henlagtÅrsak = henlagt.årsak,
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
    fun oppdaterResultatPåBehandling(behandlingId: UUID, behandlingResultat: BehandlingResultat): Behandling {
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

    fun utledNesteBehandlingstype(fagsakId: UUID): BehandlingType {
        val behandlinger = hentBehandlinger(fagsakId)
        return if (behandlinger.all { it.resultat == BehandlingResultat.HENLAGT }) BehandlingType.FØRSTEGANGSBEHANDLING else BehandlingType.REVURDERING
    }
}
