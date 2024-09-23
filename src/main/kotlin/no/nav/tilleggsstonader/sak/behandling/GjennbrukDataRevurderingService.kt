package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.NyttBarnId
import no.nav.tilleggsstonader.sak.behandling.barn.TidligereBarnId
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GjennbrukDataRevurderingService(
    private val behandlingService: BehandlingService,
    private val barnService: BarnService,
    private val vilkårperiodeService: VilkårperiodeService,
    private val stønadsperiodeService: StønadsperiodeService,
    private val vilkårService: VilkårService,
) {

    @Transactional
    fun gjenbrukData(behandling: Behandling, behandlingIdForGjenbruk: BehandlingId) {
        val barnIder: Map<TidligereBarnId, NyttBarnId> =
            barnService.gjenbrukBarn(forrigeBehandlingId = behandlingIdForGjenbruk, nyBehandlingId = behandling.id)

        gjenbrukData(behandling, behandlingIdForGjenbruk, barnIder)
    }

    /**
     * Når man nullstiller må man sende inn barnen som skal gjenbrukes
     */
    fun gjenbrukData(
        behandling: Behandling,
        behandlingIdForGjenbruk: BehandlingId,
        barnIder: Map<TidligereBarnId, NyttBarnId>,
    ) {
        vilkårperiodeService.gjenbrukVilkårperioder(
            forrigeBehandlingId = behandlingIdForGjenbruk,
            nyBehandlingId = behandling.id,
        )

        stønadsperiodeService.gjenbrukStønadsperioder(
            forrigeBehandlingId = behandlingIdForGjenbruk,
            nyBehandlingId = behandling.id,
        )

        vilkårService.kopierVilkårsettTilNyBehandling(
            forrigeBehandlingId = behandlingIdForGjenbruk,
            nyBehandling = behandling,
            barnIdMap = barnIder,
        )
    }

    fun finnBehandlingIdForGjenbruk(behandling: Behandling): BehandlingId? {
        return behandling.forrigeBehandlingId
            ?: finnSisteFerdigstilteBehandlingSomIkkeErHenlagt(behandling.fagsakId)
    }

    fun finnBehandlingIdForGjenbruk(fagsakId: FagsakId): BehandlingId? {
        return behandlingService.finnSisteIverksatteBehandling(fagsakId)?.id
            ?: finnSisteFerdigstilteBehandlingSomIkkeErHenlagt(fagsakId)
    }

    /**
     * Returnerer en map som mapper tidligere barnId til nytt barnId
     */
    fun finnNyttIdForBarn(
        nyBehandlingId: BehandlingId,
        forrigeBehandlingId: BehandlingId,
    ): Map<TidligereBarnId, NyttBarnId> {
        val nyeBarn = barnService.finnBarnPåBehandling(nyBehandlingId).associateBy { it.ident }
        val tidligereBarn = barnService.finnBarnPåBehandling(forrigeBehandlingId)
        return tidligereBarn.associate {
            val nyttBarnId = nyeBarn[it.ident]
                ?: error("Finner ikke barn som er lik ${it.id} i nyBehandling=$nyBehandlingId")
            it.id to nyttBarnId.id
        }
    }

    private fun finnSisteFerdigstilteBehandlingSomIkkeErHenlagt(fagsakId: FagsakId): BehandlingId? {
        return behandlingService.hentBehandlinger(fagsakId).lastOrNull {
            it.status == BehandlingStatus.FERDIGSTILT &&
                it.resultat != BehandlingResultat.HENLAGT
        }?.id
    }
}
