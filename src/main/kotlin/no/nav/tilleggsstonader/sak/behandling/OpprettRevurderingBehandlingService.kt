package no.nav.tilleggsstonader.sak.behandling

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.sisteFerdigstilteBehandling
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.dto.OpprettBehandlingDto
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
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
    val vilkårService: VilkårService,
    val unleashService: UnleashService,
    val gjenbrukDataRevurderingService: GjennbrukDataRevurderingService,
) {

    @Transactional
    fun opprettBehandling(request: OpprettBehandlingDto): UUID {
        feilHvisIkke(unleashService.isEnabled(Toggle.KAN_OPPRETTE_REVURDERING)) {
            "Feature toggle for å kunne opprette revurdering er slått av"
        }

        val fagsakId = request.fagsakId
        val behandling = behandlingService.opprettBehandling(
            fagsakId = fagsakId,
            behandlingsårsak = request.behandlingsårsak,
            kravMottatt = null, // TODO flytt til request
        )

        val forrigeBehandlingId = behandling.forrigeBehandlingId ?: sisteAvsluttetBehandlingId(fagsakId)

        gjenbrukDataRevurderingService.gjenbrukData(behandling, forrigeBehandlingId)

        taskService.save(
            OpprettOppgaveForOpprettetBehandlingTask.opprettTask(
                OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                    behandlingId = behandling.id,
                    saksbehandler = SikkerhetContext.hentSaksbehandler(),
                    beskrivelse = "Skal behandles i TS-Sak",
                ),
            ),
        )

        return behandling.id
    }

    private fun sisteAvsluttetBehandlingId(fagsakId: UUID): UUID {
        return behandlingService.hentBehandlinger(fagsakId)
            .sisteFerdigstilteBehandling()
            ?.id
            ?: throw Feil("Finner ikke forrige behandling for fagsak=$fagsakId")
    }
}
