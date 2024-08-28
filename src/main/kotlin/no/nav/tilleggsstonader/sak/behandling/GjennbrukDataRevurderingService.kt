package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.NyttBarnId
import no.nav.tilleggsstonader.sak.behandling.barn.TidligereBarnId
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class GjennbrukDataRevurderingService(
    private val behandlingService: BehandlingService,
    private val barnService: BarnService,
    private val vilkårperiodeService: VilkårperiodeService,
    private val stønadsperiodeService: StønadsperiodeService,
    private val vilkårService: VilkårService,
) {

    @Transactional
    fun gjenbrukData(behandling: Behandling, behandlingIdForGjenbruk: UUID) {
        val barnIder: Map<TidligereBarnId, NyttBarnId> =
            barnService.gjenbrukBarn(forrigeBehandlingId = behandlingIdForGjenbruk, nyBehandlingId = behandling.id)

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
            stønadstype = Stønadstype.BARNETILSYN, // TODO: Hent stønadstype fra behandling
        )
    }

    fun finnBehandlingIdForGjenbruk(behandling: Behandling): UUID? {
        return behandling.forrigeBehandlingId
            ?: finnSisteFerdigstilteBehandlingSomIkkeErHenlagt(behandling.fagsakId)
    }

    fun finnBehandlingIdForGjenbruk(fagsakId: UUID): UUID? {
        return behandlingService.finnSisteIverksatteBehandling(fagsakId)?.id
            ?: finnSisteFerdigstilteBehandlingSomIkkeErHenlagt(fagsakId)
    }

    private fun finnSisteFerdigstilteBehandlingSomIkkeErHenlagt(fagsakId: UUID): UUID? {
        return behandlingService.hentBehandlinger(fagsakId).lastOrNull {
            it.status == BehandlingStatus.FERDIGSTILT &&
                it.resultat != BehandlingResultat.HENLAGT
        }?.id
    }
}
