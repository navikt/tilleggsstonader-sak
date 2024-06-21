package no.nav.tilleggsstonader.sak.behandling.manuell

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.Journalposttype
import no.nav.tilleggsstonader.sak.behandling.manuell.SøknadTilsynBarnSendInnFyllUtUtil.parseInfoFraSøknad
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.identer
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OpprettBehandlingFraJournalpostService(
    private val personService: PersonService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val journalpostService: JournalpostService,
    private val taskService: TaskService,
    private val tilgangService: TilgangService,
    private val barnService: BarnService,
    private val unleashService: UnleashService,
) {
    @Transactional
    fun opprettBehandlingFraJournalpost(journalpostId: String): UUID {
        brukerfeilHvisIkke(unleashService.isEnabled(Toggle.KAN_OPPRETTE_BEHANDLING_FRA_JOURNALPOST)) {
            "Feature toggle for å kunne opprette behandling fra journalpost er slått av"
        }

        val info = hentInformasjon(journalpostId)

        val fagsak = fagsakService.hentEllerOpprettFagsak(info.ident, Stønadstype.BARNETILSYN)

        val behandling = behandlingService.opprettBehandling(
            fagsakId = fagsak.id,
            behandlingsårsak = BehandlingÅrsak.MANUELT_OPPRETTET,
        )
        val behandlingBarn =
            info.barnIdenterFraSøknad.map { BehandlingBarn(behandlingId = behandling.id, ident = it) }
        barnService.opprettBarn(behandlingBarn)

        behandlingService.leggTilBehandlingsjournalpost(journalpostId, Journalposttype.I, behandling.id)

        opprettBehandleSakOppgave(behandling)
        return behandling.id
    }

    fun hentInformasjon(journalpostId: String): OpprettBehandlingFraJournalpostStatus {
        val journalpost = journalpostService.hentJournalpost(journalpostId)

        val ident = journalpost.bruker?.takeIf { it.type == BrukerIdType.FNR }?.id
            ?: error("Finner ikke ident på journalpost=$journalpostId")
        tilgangService.validerTilgangTilPerson(ident, AuditLoggerEvent.CREATE)

        val søknadsinformasjon = hentSøknadsinformasjon(journalpost)

        val personIdenter = personService.hentPersonIdenter(ident)
        val gjeldendeIdent = personIdenter.gjeldende().ident
        validerIdentErGyldig(personIdenter.identer(), søknadsinformasjon.ident)
        validerBarnFraSøknad(gjeldendeIdent, søknadsinformasjon.identerBarn)

        val fagsak = fagsakService.finnFagsak(personIdenter.identer(), Stønadstype.BARNETILSYN)
        validerAtDetIkkeFinnesBehandlingFor(fagsak)

        return OpprettBehandlingFraJournalpostStatus(
            ident = gjeldendeIdent,
            barnIdenterFraSøknad = søknadsinformasjon.identerBarn,
            fagsakId = fagsak?.id,
        )
    }

    private fun hentSøknadsinformasjon(
        journalpost: Journalpost,
    ): Søknadsinformasjon {
        val brevkode = "${DokumentBrevkode.BARNETILSYN}B"
        val dokumentJournalpost = journalpost.dokumenter?.singleOrNull { it.brevkode == brevkode }
            ?: error("Finner ikke dokument med brevkode=$brevkode i journalpost=${journalpost.journalpostId}")
        val dokument = journalpostService.hentDokument(
            journalpost = journalpost,
            dokumentInfoId = dokumentJournalpost.dokumentInfoId,
            dokumentVariantformat = Dokumentvariantformat.ORIGINAL,
        )
        return parseInfoFraSøknad(dokument)
    }

    private fun validerIdentErGyldig(identer: Set<String>, identFraSøknad: String) {
        feilHvisIkke(identer.contains(identFraSøknad)) {
            "Ident i søknad tilsvarer ikke ident på personen tilknyttet journalposten"
        }
    }

    private fun opprettBehandleSakOppgave(behandling: Behandling) {
        val task = OpprettOppgaveForOpprettetBehandlingTask.opprettTask(
            OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                behandlingId = behandling.id,
                saksbehandler = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
                beskrivelse = "Manuelt opprettet sak fra journalpost. Skal saksbehandles i ny løsning.",
            ),
        )
        taskService.save(task)
    }

    /**
     * Tillater kun å opprette førstegangsbehandling på personen
     */
    private fun validerAtDetIkkeFinnesBehandlingFor(fagsak: Fagsak?) {
        val behandlinger = fagsak?.let { behandlingService.hentBehandlinger(it.id) } ?: emptyList()
        feilHvis(behandlinger.isNotEmpty()) {
            "Det finnes allerede en behandling på personen"
        }
    }

    private fun validerBarnFraSøknad(gjeldendeIdent: String, barnIdenterFraSøknad: Set<String>) {
        feilHvis(barnIdenterFraSøknad.isEmpty()) {
            "Søknaden mangler identer på barn - kan ikke opprette behandling manuelt uten barn"
        }
        val søker = personService.hentPersonMedBarn(gjeldendeIdent)
        barnIdenterFraSøknad.forEach { it ->
            feilHvisIkke(søker.barn.containsKey(it)) {
                "Søknaden inneholder barn $it som ikke finnes på personen=$gjeldendeIdent"
            }
        }
    }

    data class OpprettBehandlingFraJournalpostStatus(
        val ident: String,
        val barnIdenterFraSøknad: Set<String>,
        val fagsakId: UUID?,
    )
}
