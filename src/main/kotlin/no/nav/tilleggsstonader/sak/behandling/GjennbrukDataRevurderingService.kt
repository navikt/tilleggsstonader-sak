package no.nav.tilleggsstonader.sak.behandling

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.NyttBarnId
import no.nav.tilleggsstonader.sak.behandling.barn.TidligereBarnId
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class GjennbrukDataRevurderingService(
    val taskService: TaskService,
    val barnService: BarnService,
    val vilkårperiodeService: VilkårperiodeService,
    val stønadsperiodeService: StønadsperiodeService,
    val vilkårService: VilkårService,
    val unleashService: UnleashService,
) {

    @Transactional
    fun gjenbrukData(behandling: Behandling, gjennbrukDataFraBehandlingId: UUID) {
        // TODO skal vi kopiere fra forrige henlagte/avslåtte? Hva hvis behandlingen før er innvilget.

        val barnIder: Map<TidligereBarnId, NyttBarnId> =
            barnService.gjenbrukBarn(forrigeBehandlingId = gjennbrukDataFraBehandlingId, nyBehandlingId = behandling.id)

        vilkårperiodeService.gjenbrukVilkårperioder(
            forrigeBehandlingId = gjennbrukDataFraBehandlingId,
            nyBehandlingId = behandling.id,
        )

        stønadsperiodeService.gjenbrukStønadsperioder(
            forrigeBehandlingId = gjennbrukDataFraBehandlingId,
            nyBehandlingId = behandling.id,
        )

        vilkårService.kopierVilkårsettTilNyBehandling(
            forrigeBehandlingId = gjennbrukDataFraBehandlingId,
            nyBehandling = behandling,
            barnIdMap = barnIder,
            stønadstype = Stønadstype.BARNETILSYN, // TODO: Hent stønadstype fra behandling
        )
    }
}
