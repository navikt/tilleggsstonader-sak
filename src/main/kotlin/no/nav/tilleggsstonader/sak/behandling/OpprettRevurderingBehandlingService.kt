package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.sisteFerdigstilteBehandling
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.NyttBarnId
import no.nav.tilleggsstonader.sak.behandling.barn.TidligereBarnId
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.dto.OpprettBehandlingDto
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class OpprettRevurderingBehandlingService(
    val behandlingService: BehandlingService,
    val barnService: BarnService,
) {

    @Transactional
    fun opprettBehandling(request: OpprettBehandlingDto): UUID {
        val behandling = behandlingService.opprettBehandling(
            fagsakId = request.fagsakId,
            behandlingsårsak = BehandlingÅrsak.NYE_OPPLYSNINGER, // TODO flytt til request
            kravMottatt = null, // TODO flytt til request
        )

        gjenbrukData(behandling)

        return behandling.id
    }

    private fun gjenbrukData(behandling: Behandling) {
        // TODO skal vi kopiere fra forrige henlagte/avslåtte? Hva hvis behandlingen før er innvilget.
        val forrigeBehandlingId = behandling.forrigeBehandlingId ?: sisteAvsluttetBehandlingId(behandling)

        val barnIder: Map<TidligereBarnId, NyttBarnId> =
            barnService.gjenbrukBarn(forrigeBehandlingId = forrigeBehandlingId, nyBehandlingId = behandling.id)

        // TODO kopier vilkårperioder
        // TODO kopier stønadsperioder
        // TODO kopier vilkår
    }

    private fun sisteAvsluttetBehandlingId(behandling: Behandling): UUID {
        return behandlingService.hentBehandlinger(behandling.fagsakId)
            .sisteFerdigstilteBehandling()
            ?.id
            ?: throw Feil("Finner ikke forrige behandling for fagsak=${behandling.fagsakId}")
    }
}
