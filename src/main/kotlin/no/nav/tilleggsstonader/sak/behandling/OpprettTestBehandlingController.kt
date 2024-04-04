package no.nav.tilleggsstonader.sak.behandling

import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.søknad.DatoFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFlereValgFelt
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.SelectFelt
import no.nav.tilleggsstonader.kontrakter.søknad.Søknadsskjema
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaBarnetilsyn
import no.nav.tilleggsstonader.kontrakter.søknad.TekstFelt
import no.nav.tilleggsstonader.kontrakter.søknad.VerdiFelt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.AktivitetAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.ArbeidOgOpphold
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.BarnAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.BarnMedBarnepass
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.HovedytelseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.MottarPengestøtteTyper
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.OppholdUtenforNorge
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.TypeBarnepass
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.ÅrsakOppholdUtenforNorge
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
import org.springframework.context.annotation.Profile
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/test/opprett-behandling"])
@ProtectedWithClaims(issuer = "azuread")
@Profile("!prod")
class OpprettTestBehandlingController(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val personService: PersonService,
    private val barnService: BarnService,
    private val søknadService: SøknadService,
    private val taskService: TaskService,
) {

    @Transactional
    @PostMapping
    fun opprettBehandling(@RequestBody testBehandlingRequest: TestBehandlingRequest): UUID {
        val fagsak: Fagsak = lagFagsak(testBehandlingRequest)
        val behandling = lagBehandling(fagsak)
        opprettSøknad(fagsak, behandling)
        opprettOppgave(behandling)

        return behandling.id
    }

    private fun lagBehandling(fagsak: Fagsak): Behandling =
        behandlingService.opprettBehandling(
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            fagsakId = fagsak.id,
            behandlingsårsak = BehandlingÅrsak.SØKNAD,
        )

    private fun opprettSøknad(fagsak: Fagsak, behandling: Behandling) {
        when (fagsak.stønadstype) {
            Stønadstype.BARNETILSYN -> opprettSøknadBarnetilsyn(fagsak, behandling)
        }
    }

    private fun opprettSøknadBarnetilsyn(
        fagsak: Fagsak,
        behandling: Behandling,
    ) {
        val pdlBarn = personService.hentPersonMedBarn(fagsak.hentAktivIdent()).barn
        val barnMedBarnepass = pdlBarn.entries.map { (ident, barn) ->
            BarnMedBarnepass(
                ident = TekstFelt("", ident),
                navn = TekstFelt("", "navn"),
                type = EnumFelt("", TypeBarnepass.BARNEHAGE_SFO_AKS, "", emptyList()),
                startetIFemte = null,
                årsak = null,
            )
        }
        val skjemaBarnetilsyn = SøknadsskjemaBarnetilsyn(
            hovedytelse = HovedytelseAvsnitt(
                hovedytelse = EnumFlereValgFelt("", listOf(VerdiFelt(Hovedytelse.AAP, "AAP")), emptyList()),
                arbeidOgOpphold = arbeidOgOpphold(),
            ),
            aktivitet = AktivitetAvsnitt(
                utdanning = EnumFelt("", JaNei.JA, "", emptyList()),
            ),
            barn = BarnAvsnitt(
                barnMedBarnepass = barnMedBarnepass,
            ),
            dokumentasjon = emptyList(),
        )
        val skjema = Søknadsskjema(
            ident = fagsak.hentAktivIdent(),
            mottattTidspunkt = LocalDateTime.now(),
            språk = Språkkode.NB,
            skjema = skjemaBarnetilsyn,
        )
        val journalpost = Journalpost("TESTJPID", Journalposttype.I, Journalstatus.FERDIGSTILT)
        val søknad = søknadService.lagreSøknad(behandling.id, journalpost, skjema)
        opprettBarn(behandling, søknad)
    }

    private fun arbeidOgOpphold() = ArbeidOgOpphold(
        jobberIAnnetLand = EnumFelt("Jobber du i et annet land enn Norge?", JaNei.JA, "Ja", emptyList()),
        jobbAnnetLand = SelectFelt("Hvilket land jobber du i?", "SWE", "Sverige"),
        harPengestøtteAnnetLand = EnumFlereValgFelt(
            "Mottar du pengestøttene fra et annet land enn Norge?",
            listOf(
                VerdiFelt(
                    MottarPengestøtteTyper.SYKEPENGER,
                    "Sykepenger",
                ),
            ),
            emptyList(),
        ),
        pengestøtteAnnetLand = SelectFelt("Hvilket land mottar du pengestøtte fra?", "SWE", "Sverige"),
        harOppholdUtenforNorgeSiste12mnd = EnumFelt("Jobber du i et annet land enn Norge?", JaNei.JA, "Ja", emptyList()),
        oppholdUtenforNorgeSiste12mnd = listOf(oppholdUtenforNorge()),
        harOppholdUtenforNorgeNeste12mnd = EnumFelt("Jobber du i et annet land enn Norge?", JaNei.JA, "Ja", emptyList()),
        oppholdUtenforNorgeNeste12mnd = listOf(oppholdUtenforNorge()),
    )

    private fun oppholdUtenforNorge() = OppholdUtenforNorge(
        land = SelectFelt("Hvilket land har du oppholdt deg i?", "SWE", "Sverige"),
        årsak = EnumFlereValgFelt(
            "Hva gjorde du i dette landet?",
            listOf(VerdiFelt(ÅrsakOppholdUtenforNorge.JOBB, "Jobb")),
            alternativer = emptyList(),
        ),
        fom = DatoFelt("Fom", LocalDate.of(2024, 1, 1)),
        tom = DatoFelt("Fom", LocalDate.of(2024, 1, 1)),
    )

    // Oppretter BehandlingBarn for alle barn fra PDL for å få et vilkår per barn
    private fun opprettBarn(behandling: Behandling, søknad: SøknadBarnetilsyn) {
        val behandlingBarn = søknad.barn.map { barn ->
            BehandlingBarn(
                behandlingId = behandling.id,
                ident = barn.ident,
                søknadBarnId = barn.id,
            )
        }
        barnService.opprettBarn(behandlingBarn)
    }

    private fun opprettOppgave(behandling: Behandling) {
        taskService.save(
            OpprettOppgaveForOpprettetBehandlingTask.opprettTask(
                OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                    behandlingId = behandling.id,
                    saksbehandler = SikkerhetContext.hentSaksbehandler(),
                    beskrivelse = "Testbehandling (ikke lagd med en ekte søknad)",
                ),
            ),
        )
    }

    private fun lagFagsak(testBehandlingRequest: TestBehandlingRequest) =
        fagsakService.hentEllerOpprettFagsak(testBehandlingRequest.personIdent, Stønadstype.BARNETILSYN)
}

data class TestBehandlingRequest(
    val personIdent: String,
    val stønadstype: Stønadstype = Stønadstype.BARNETILSYN,
)
