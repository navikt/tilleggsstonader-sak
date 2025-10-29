package no.nav.tilleggsstonader.sak.behandling

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgavePrioritet
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.OpprettRevurdering
import no.nav.tilleggsstonader.sak.behandling.dto.BarnTilRevurderingDto
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlBarn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gjeldende
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.visningsnavn
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class OpprettRevurderingService(
    private val taskService: TaskService,
    private val opprettBehandlingService: OpprettBehandlingService,
    private val barnService: BarnService,
    private val unleashService: UnleashService,
    private val gjenbrukDataRevurderingService: GjenbrukDataRevurderingService,
    private val personService: PersonService,
    private val fagsakService: FagsakService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun opprettRevurdering(opprettRevurdering: OpprettRevurdering): BehandlingId {
        feilHvisIkke(unleashService.isEnabled(Toggle.KAN_OPPRETTE_REVURDERING)) {
            "Feature toggle for å kunne opprette revurdering er slått av"
        }
        logger.info("Oppretter revurdering for fagsak=${opprettRevurdering.fagsakId}")

        if (opprettRevurdering.årsak == BehandlingÅrsak.NYE_OPPLYSNINGER) {
            feilHvis(opprettRevurdering.nyeOpplysningerMetadata == null) {
                "Krever metadata ved behandlingsårsak NYE_OPPLYSNINGER"
            }
        }

        val behandling = opprettBehandlingService.opprettBehandling(lagOpprettBehandlingRequest(opprettRevurdering))

        val behandlingIdForGjenbruk = gjenbrukDataRevurderingService.finnBehandlingIdForGjenbruk(behandling)

        validerValgteBarn(opprettRevurdering, behandlingIdForGjenbruk)

        behandlingIdForGjenbruk?.let { gjenbrukDataRevurderingService.gjenbrukData(behandling, it) }
        barnService.opprettBarn(opprettRevurdering.valgteBarn.map { BehandlingBarn(behandlingId = behandling.id, ident = it) })

        return behandling.id
    }

    private fun lagOpprettBehandlingRequest(opprettRevurdering: OpprettRevurdering): OpprettBehandlingRequest =
        if (opprettRevurdering.skalOppretteOppgave) {
            OpprettBehandlingRequest(
                fagsakId = opprettRevurdering.fagsakId,
                behandlingsårsak = opprettRevurdering.årsak,
                kravMottatt = opprettRevurdering.kravMottatt,
                nyeOpplysningerMetadata = opprettRevurdering.nyeOpplysningerMetadata,
                oppgaveMetadata =
                    OpprettBehandlingOppgaveMetadata(
                        tilordneSaksbehandler = SikkerhetContext.hentSaksbehandler(),
                        beskrivelse = "Skal behandles i TS-Sak",
                        prioritet = OppgavePrioritet.NORM,
                    ),
            )
        } else {
            OpprettBehandlingRequest(
                fagsakId = opprettRevurdering.fagsakId,
                behandlingsårsak = opprettRevurdering.årsak,
                kravMottatt = opprettRevurdering.kravMottatt,
                nyeOpplysningerMetadata = opprettRevurdering.nyeOpplysningerMetadata,
                skalOppretteOppgave = false,
                oppgaveMetadata = null,
            )
        }

    private fun validerValgteBarn(
        request: OpprettRevurdering,
        behandlingIdForGjenbruk: BehandlingId?,
    ) {
        val stønadstype: Stønadstype by lazy { fagsakService.hentFagsak(request.fagsakId).stønadstype }
        feilHvis(request.valgteBarn.isNotEmpty() && stønadstype != Stønadstype.BARNETILSYN) {
            "Kan ikke sende inn barn til $stønadstype"
        }
        if (stønadstype != Stønadstype.BARNETILSYN) {
            return
        }

        feilHvis(
            !request.årsak.erSøknadEllerPapirsøknad() &&
                request.årsak != BehandlingÅrsak.KORRIGERING_UTEN_BREV &&
                request.valgteBarn.isNotEmpty(),
        ) {
            "Kan ikke sende med barn på annet enn årsak Søknad"
        }

        val barnTilRevurdering = hentBarnTilRevurdering(request.fagsakId, behandlingIdForGjenbruk).barn

        val valgbareIdenter = barnTilRevurdering.filterNot { it.finnesPåForrigeBehandling }.map { it.ident }
        feilHvis(!valgbareIdenter.containsAll(request.valgteBarn)) {
            "Kan ikke velge barn som ikke er valgbare."
        }
        brukerfeilHvis(behandlingIdForGjenbruk == null && request.valgteBarn.isEmpty()) {
            "Behandling må opprettes med minimum 1 barn. Dersom alle tidligere behandlinger er henlagt, må ny behandling opprettes som søknad eller papirsøknad."
        }
    }

    fun hentBarnTilRevurdering(fagsakId: FagsakId): BarnTilRevurderingDto {
        val forrigeIverksatteBehandlingId = gjenbrukDataRevurderingService.finnBehandlingIdForGjenbruk(fagsakId)
        return hentBarnTilRevurdering(fagsakId, forrigeIverksatteBehandlingId)
    }

    private fun hentBarnTilRevurdering(
        fagsakId: FagsakId,
        forrigeIverksatteBehandlingId: BehandlingId?,
    ): BarnTilRevurderingDto {
        val ident = fagsakService.hentAktivIdent(fagsakId)
        val barnPåSøker = personService.hentPersonMedBarn(ident).barn
        val eksisterendeBarn =
            forrigeIverksatteBehandlingId
                ?.let { barnService.finnBarnPåBehandling(forrigeIverksatteBehandlingId).associateBy { it.ident } }
                ?: emptyMap()

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
}
