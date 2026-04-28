package no.nav.tilleggsstonader.sak.behandling.opprettelse.dummy

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgavePrioritet
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.opprettelse.OpprettBehandling
import no.nav.tilleggsstonader.sak.behandling.opprettelse.OpprettBehandlingOppgaveMetadata
import no.nav.tilleggsstonader.sak.behandling.opprettelse.OpprettBehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.context.annotation.Profile
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/test/opprett-behandling"])
@ProtectedWithClaims(issuer = "azuread")
@Profile("!prod")
class OpprettDummyBehandlingController(
    private val tilgangService: TilgangService,
    private val opprettBehandlingService: OpprettBehandlingService,
    private val fagsakService: FagsakService,
    private val opprettDummySøknadBarnetilsyn: OpprettDummySøknadBarnetilsyn,
    private val opprettDummySøknadLæremidler: OpprettDummySøknadLæremidler,
    private val opprettDummySøknadBoutgifter: OpprettDummySøknadBoutgifter,
    private val opprettDummySøknadDagligReise: OpprettDummySøknadDagligReise,
    private val opprettDummyBehandlingReiseTilSamling: OpprettDummyBehandlingReiseTilSamling,
) {
    @Transactional
    @PostMapping
    fun opprettBehandling(
        @RequestBody testBehandlingRequest: TestBehandlingRequest,
    ): BehandlingId {
        tilgangService.validerTilgangTilStønadstype(
            testBehandlingRequest.personIdent,
            testBehandlingRequest.stønadstype,
            AuditLoggerEvent.CREATE,
        )

        val fagsak: Fagsak = lagFagsak(testBehandlingRequest)
        val behandling = lagBehandling(fagsak)
        opprettSøknad(fagsak, behandling)

        return behandling.id
    }

    private fun lagBehandling(fagsak: Fagsak): Behandling =
        opprettBehandlingService.opprettBehandling(
            OpprettBehandling(
                fagsakId = fagsak.id,
                behandlingsårsak = BehandlingÅrsak.SØKNAD,
                oppgaveMetadata =
                    OpprettBehandlingOppgaveMetadata.OppgaveMetadata(
                        tilordneSaksbehandler = SikkerhetContext.hentSaksbehandler(),
                        beskrivelse = "Testbehandling (ikke lagd med en ekte søknad)",
                        prioritet = OppgavePrioritet.NORM,
                    ),
            ),
        )

    private fun opprettSøknad(
        fagsak: Fagsak,
        behandling: Behandling,
    ) {
        when (fagsak.stønadstype) {
            Stønadstype.BARNETILSYN -> opprettDummySøknadBarnetilsyn.opprettDummy(fagsak, behandling)
            Stønadstype.LÆREMIDLER -> opprettDummySøknadLæremidler.opprettDummy(fagsak, behandling)
            Stønadstype.BOUTGIFTER -> opprettDummySøknadBoutgifter.opprettDummy(fagsak, behandling)
            Stønadstype.DAGLIG_REISE_TSO -> opprettDummySøknadDagligReise.opprettDummy(fagsak, behandling)
            Stønadstype.DAGLIG_REISE_TSR -> opprettDummySøknadDagligReise.opprettDummy(fagsak, behandling)
            Stønadstype.REISE_TIL_SAMLING_TSO -> opprettDummyBehandlingReiseTilSamling.opprettDummy(fagsak, behandling)
        }
    }

    private fun lagFagsak(testBehandlingRequest: TestBehandlingRequest) =
        fagsakService.hentEllerOpprettFagsak(testBehandlingRequest.personIdent, testBehandlingRequest.stønadstype)
}

data class TestBehandlingRequest(
    val personIdent: String,
    val stønadstype: Stønadstype,
)
