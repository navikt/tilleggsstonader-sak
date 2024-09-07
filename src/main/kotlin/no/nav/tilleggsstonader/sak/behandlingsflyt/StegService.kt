package no.nav.tilleggsstonader.sak.behandlingsflyt

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.Behandlingshistorikk
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegValidering.validerGyldigTilstand
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegValidering.validerHarTilgang
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegValidering.validerRollerForResetSteg
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.RolleConfig
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringSteg
import no.nav.tilleggsstonader.sak.vilkår.InngangsvilkårSteg
import no.nav.tilleggsstonader.sak.vilkår.VilkårSteg
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StegService(
    private val behandlingService: BehandlingService,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val rolleConfig: RolleConfig,
    private val behandlingSteg: List<BehandlingSteg<*>>,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val metrics = StegMetrics(behandlingSteg)

    @Transactional
    fun resetSteg(behandlingId: BehandlingId, steg: StegType) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        if (behandling.status != BehandlingStatus.UTREDES) {
            error("Kan ikke resette steg når status=${behandling.status} behandling=$behandlingId")
        }
        if (steg.kommerEtter(behandling.steg)) {
            error(
                "Kan ikke resette behandling til steg=$steg når behandling allerede " +
                    "er på ${behandling.steg} behandling=$behandlingId",
            )
        }

        validerRollerForResetSteg(rolleConfig, behandling, steg)
        behandlingService.oppdaterStegPåBehandling(behandlingId, steg)
    }

    @Transactional
    fun håndterSteg(
        behandlingId: BehandlingId,
        behandlingSteg: BehandlingSteg<Void?>,
    ): Behandling {
        return håndterSteg(
            behandlingService.hentSaksbehandling(behandlingId),
            behandlingSteg,
            null,
        )
    }

    @Transactional
    fun håndterSteg(behandlingId: BehandlingId, steg: StegType): Behandling {
        val behandling = behandlingService.hentBehandling(behandlingId)

        feilHvis(behandling.steg != steg) {
            "Behandling er i steg ${behandling.steg}. Steget $steg kan derfor ikke ferdigstilles."
        }

        return when (steg) {
            StegType.INNGANGSVILKÅR -> håndterInngangsvilkår(behandlingId)
            StegType.VILKÅR -> håndterVilkår(behandlingId)
            StegType.SIMULERING -> håndterSimulering(behandlingId)
            else -> error("Steg $steg kan ikke ferdigstilles her")
        }
    }

    private fun håndterInngangsvilkår(
        behandlingId: BehandlingId,
    ): Behandling {
        val inngangsvilkårSteg: InngangsvilkårSteg = behandlingSteg.filterIsInstance<InngangsvilkårSteg>().single()

        return håndterSteg(behandlingId, inngangsvilkårSteg)
    }

    private fun håndterVilkår(behandlingId: BehandlingId): Behandling {
        val vilkårSteg: VilkårSteg = behandlingSteg.filterIsInstance<VilkårSteg>().single()
        return håndterSteg(behandlingId, vilkårSteg)
    }

    private fun håndterSimulering(behandlingId: BehandlingId): Behandling {
        val simuleringSteg: SimuleringSteg = behandlingSteg.filterIsInstance<SimuleringSteg>().single()
        return håndterSteg(behandlingId, simuleringSteg)
    }

    @Transactional
    fun <T> håndterSteg(
        behandlingId: BehandlingId,
        behandlingSteg: BehandlingSteg<T>,
        data: T,
    ): Behandling {
        return håndterSteg(
            behandlingService.hentSaksbehandling(behandlingId),
            behandlingSteg,
            data,
        )
    }

    @Transactional
    fun håndterSteg(
        saksbehandling: Saksbehandling,
        behandlingSteg: BehandlingSteg<Void?>,
    ): Behandling {
        return håndterSteg(saksbehandling, behandlingSteg, null)
    }

    @Transactional
    fun <T> håndterSteg(
        saksbehandling: Saksbehandling,
        behandlingSteg: BehandlingSteg<T>,
        data: T,
    ): Behandling {
        try {
            return håndter(saksbehandling, behandlingSteg, data)
        } catch (exception: Exception) {
            val stegType = behandlingSteg.stegType()
            metrics.failure(stegType)
            logger.warn("Håndtering av stegtype '$stegType' feilet på behandling ${saksbehandling.id}.")
            throw exception
        }
    }

    private fun <T> håndter(
        saksbehandling: Saksbehandling,
        behandlingSteg: BehandlingSteg<T>,
        data: T,
    ): Behandling {
        val stegType = behandlingSteg.stegType()
        val saksbehandlerIdent = SikkerhetContext.hentSaksbehandlerEllerSystembruker()
        valider(saksbehandling, stegType, saksbehandlerIdent, behandlingSteg)

        val nesteSteg = behandlingSteg.utførOgReturnerNesteSteg(saksbehandling, data)

        oppdaterHistorikk(behandlingSteg, saksbehandling.id, saksbehandlerIdent)
        metrics.success(stegType)
        validerNesteSteg(nesteSteg, saksbehandling)
        logger.info("$stegType på behandling ${saksbehandling.id} er håndtert")
        return behandlingService.oppdaterStegPåBehandling(behandlingId = saksbehandling.id, steg = nesteSteg)
    }

    private fun <T> valider(
        saksbehandling: Saksbehandling,
        stegType: StegType,
        saksbehandlerIdent: String,
        behandlingSteg: BehandlingSteg<T>,
    ) {
        utførBehandlingsvalidering(behandlingSteg, saksbehandling)
        validerHarTilgang(rolleConfig, saksbehandling, stegType, saksbehandlerIdent)
        validerGyldigTilstand(saksbehandling, stegType, saksbehandlerIdent)
    }

    private fun <T> utførBehandlingsvalidering(
        behandlingSteg: BehandlingSteg<T>,
        saksbehandling: Saksbehandling,
    ) {
        behandlingSteg.validerSteg(saksbehandling)
        feilHvis(!behandlingSteg.stegType().erGyldigIKombinasjonMedStatus(saksbehandling.status)) {
            "Kan ikke utføre '${
                behandlingSteg.stegType().displayName()
            }' når behandlingstatus er ${saksbehandling.status.visningsnavn()}"
        }
    }

    private fun validerNesteSteg(nesteSteg: StegType, saksbehandling: Saksbehandling) {
        if (!nesteSteg.erGyldigIKombinasjonMedStatus(behandlingService.hentBehandling(saksbehandling.id).status)) {
            error(
                "Steg '${nesteSteg.displayName()}' kan ikke settes " +
                    "på behandling i kombinasjon med status ${saksbehandling.status}",
            )
        }
    }

    private fun <T> oppdaterHistorikk(
        behandlingSteg: BehandlingSteg<T>,
        behandlingId: BehandlingId,
        saksbehandlerIdent: String,
    ) {
        if (behandlingSteg.settInnHistorikk()) {
            behandlingshistorikkService.opprettHistorikkInnslag(
                Behandlingshistorikk(
                    behandlingId = behandlingId,
                    steg = behandlingSteg.stegType(),
                    opprettetAv = saksbehandlerIdent,
                ),
            )
        }
    }
}
