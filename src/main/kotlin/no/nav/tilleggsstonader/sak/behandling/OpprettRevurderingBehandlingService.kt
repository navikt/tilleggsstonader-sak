package no.nav.tilleggsstonader.sak.behandling

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.sisteFerdigstilteBehandling
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.dto.BarnTilRevurderingDto
import no.nav.tilleggsstonader.sak.behandling.dto.OpprettBehandlingDto
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlBarn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gjeldende
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.visningsnavn
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
    val personService: PersonService,
    val fagsakService: FagsakService,
) {

    @Transactional
    fun opprettBehandling(request: OpprettBehandlingDto): UUID {
        feilHvisIkke(unleashService.isEnabled(Toggle.KAN_OPPRETTE_REVURDERING)) {
            "Feature toggle for å kunne opprette revurdering er slått av"
        }

        val fagsakId = request.fagsakId
        val behandling = behandlingService.opprettBehandling(
            fagsakId = fagsakId,
            behandlingsårsak = request.årsak,
            kravMottatt = null, // TODO flytt til request
        )

        val forrigeBehandlingId = behandling.forrigeBehandlingId ?: sisteAvsluttetBehandlingId(fagsakId)

        validerValgteBarn(request, forrigeBehandlingId)

        gjenbrukDataRevurderingService.gjenbrukData(behandling, forrigeBehandlingId)
        barnService.opprettBarn(request.valgteBarn.map { BehandlingBarn(behandlingId = behandling.id, ident = it) })

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

    private fun validerValgteBarn(request: OpprettBehandlingDto, forrigeBehandlingId: UUID) {
        val stønadstype: Stønadstype by lazy { fagsakService.hentFagsak(request.fagsakId).stønadstype }
        feilHvis(request.valgteBarn.isNotEmpty() && stønadstype != Stønadstype.BARNETILSYN) {
            "Kan ikke sende inn barn til $stønadstype"
        }

        feilHvis(request.årsak != BehandlingÅrsak.SØKNAD && request.valgteBarn.isNotEmpty()) {
            "Kan ikke sende med barn på annet enn årsak Søknad"
        }

        val barnTilRevurdering = hentBarnTilRevurdering(request.fagsakId, forrigeBehandlingId).barn

        val valgbareIdenter = barnTilRevurdering.filterNot { it.finnesPåForrigeBehandling }.map { it.ident }
        feilHvis(!valgbareIdenter.containsAll(request.valgteBarn)) {
            "Kan ikke velge barn som ikke er valgbare."
        }
    }

    fun hentBarnTilRevurdering(fagsakId: UUID): BarnTilRevurderingDto {
        val forrigeBehandlingId = behandlingService.finnSisteIverksatteBehandling(fagsakId)?.id
            ?: sisteAvsluttetBehandlingId(fagsakId)

        return hentBarnTilRevurdering(fagsakId, forrigeBehandlingId)
    }

    private fun hentBarnTilRevurdering(
        fagsakId: UUID,
        forrigeBehandlingId: UUID,
    ): BarnTilRevurderingDto {
        val ident = fagsakService.hentAktivIdent(fagsakId)
        val barnPåSøker = personService.hentPersonMedBarn(ident).barn
        val eksisterendeBarn = barnService.finnBarnPåBehandling(forrigeBehandlingId).associateBy { it.ident }

        return BarnTilRevurderingDto(
            barn = mapEksisterendeBarn(eksisterendeBarn, barnPåSøker) + mapValgbareBarn(barnPåSøker, eksisterendeBarn),
        )
    }

    private fun mapEksisterendeBarn(
        eksisterendeBarn: Map<String, BehandlingBarn>,
        barnPåSøker: Map<String, PdlBarn>,
    ) = eksisterendeBarn.values.map {
        val barn = barnPåSøker[it.ident] ?: error("Finner ikke barn=${it.ident} på søker")
        BarnTilRevurderingDto.Barn(
            ident = it.ident,
            navn = barn.navn.gjeldende().visningsnavn(),
            finnesPåForrigeBehandling = true,
        )
    }

    private fun mapValgbareBarn(
        barnPåSøker: Map<String, PdlBarn>,
        eksisterendeBarn: Map<String, BehandlingBarn>,
    ) = barnPåSøker
        .filter { (ident, _) -> !eksisterendeBarn.containsKey(ident) }
        .map { (ident, barn) ->
            BarnTilRevurderingDto.Barn(
                ident = ident,
                navn = barn.navn.gjeldende().visningsnavn(),
                finnesPåForrigeBehandling = false,
            )
        }

    private fun sisteAvsluttetBehandlingId(fagsakId: UUID): UUID {
        return behandlingService.hentBehandlinger(fagsakId)
            .sisteFerdigstilteBehandling()
            ?.id
            ?: throw Feil("Finner ikke forrige behandling for fagsak=$fagsakId")
    }
}
