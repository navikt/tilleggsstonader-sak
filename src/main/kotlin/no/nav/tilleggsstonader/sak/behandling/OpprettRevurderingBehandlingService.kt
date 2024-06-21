package no.nav.tilleggsstonader.sak.behandling

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.sisteFerdigstilteBehandling
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.NyttBarnId
import no.nav.tilleggsstonader.sak.behandling.barn.TidligereBarnId
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.dto.OpprettBehandlingDto
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OpprettRevurderingBehandlingService(
    val taskService: TaskService,
    val behandlingService: BehandlingService,
    val barnService: BarnService,
    val vilkårperiodeService: VilkårperiodeService,
    val stønadsperiodeService: StønadsperiodeService,
    val unleashService: UnleashService,
) {

    @Transactional
    fun opprettBehandling(request: OpprettBehandlingDto): UUID {
        feilHvisIkke(unleashService.isEnabled(Toggle.KAN_OPPRETTE_REVURDERING)) {
            "Feature toggle for å kunne opprette revurdering er slått av"
        }

        val behandling = behandlingService.opprettBehandling(
            fagsakId = request.fagsakId,
            behandlingsårsak = BehandlingÅrsak.NYE_OPPLYSNINGER, // TODO flytt til request
            kravMottatt = null, // TODO flytt til request
        )

        gjenbrukData(behandling)
        taskService.save(
            OpprettOppgaveForOpprettetBehandlingTask.opprettTask(
                OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                    behandlingId = behandling.id,
                    saksbehandler = SikkerhetContext.hentSaksbehandler(),
                    beskrivelse = "Revurdering. Skal saksbehandles i ny løsning.", // TODO tekst
                ),
            ),
        )

        return behandling.id
    }

    private fun gjenbrukData(behandling: Behandling) {
        // TODO skal vi kopiere fra forrige henlagte/avslåtte? Hva hvis behandlingen før er innvilget.
        val forrigeBehandlingId = behandling.forrigeBehandlingId ?: sisteAvsluttetBehandlingId(behandling)

        val barnIder: Map<TidligereBarnId, NyttBarnId> =
            barnService.gjenbrukBarn(forrigeBehandlingId = forrigeBehandlingId, nyBehandlingId = behandling.id)

        vilkårperiodeService.gjenbrukVilkårperioder(
            forrigeBehandlingId = forrigeBehandlingId,
            nyBehandlingId = behandling.id,
        )

        stønadsperiodeService.gjenbrukStønadsperioder(
            forrigeBehandlingId = forrigeBehandlingId,
            nyBehandlingId = behandling.id,
        )

        // TODO kopier vilkår
    }

    private fun sisteAvsluttetBehandlingId(behandling: Behandling): UUID {
        return behandlingService.hentBehandlinger(behandling.fagsakId)
            .sisteFerdigstilteBehandling()
            ?.id
            ?: throw Feil("Finner ikke forrige behandling for fagsak=${behandling.fagsakId}")
    }
}
