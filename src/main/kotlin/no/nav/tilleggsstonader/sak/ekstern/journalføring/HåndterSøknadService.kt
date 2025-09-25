package no.nav.tilleggsstonader.sak.ekstern.journalføring

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.felles.gjelderDagligReise
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.kontrakter.sak.journalføring.HåndterSøknadRequest
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService.Companion.MASKINELL_JOURNALFOERENDE_ENHET
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.journalføring.JournalføringService
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import no.nav.tilleggsstonader.sak.journalføring.dokumentBrevkode
import no.nav.tilleggsstonader.sak.journalføring.gjelderKanalNavNo
import no.nav.tilleggsstonader.sak.journalføring.harStrukturertSøknad
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.OpprettOppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.identer
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadDagligReise
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.tilMålgruppeType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class HåndterSøknadService(
    private val personService: PersonService,
    private val journalpostService: JournalpostService,
    private val taskService: TaskService,
    private val journalføringService: JournalføringService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val søknadService: SøknadService,
    private val unleashService: UnleashService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun håndterSøknad(request: HåndterSøknadRequest) {
        val personIdent = request.personIdent
        val stønadstype = request.stønadstype

        val journalpost = journalpostService.hentJournalpost(request.journalpostId)
        håndterSøknad(personIdent = personIdent, stønadstype = stønadstype, journalpost = journalpost)
    }

    @Transactional
    fun håndterSøknad(journalpost: Journalpost) {
        val personIdent = journalpostService.hentIdentFraJournalpost(journalpost)
        val stønadstype =
            finnStønadstyperSomKanOpprettesFraJournalpost(journalpost).defaultStønadstype
                ?: error("Fant ikke dokument brevkode for journalpost")
        håndterSøknad(personIdent = personIdent, stønadstype = stønadstype, journalpost = journalpost)
    }

    fun finnStønadstyperSomKanOpprettesFraJournalpost(journalpost: Journalpost): ValgbareStønadstyperForJournalpost {
        val dokumentbrevkode = journalpost.dokumentBrevkode()
        if (dokumentbrevkode == null) {
            val valgbareStønadstyper =
                if (unleashService.isEnabled(Toggle.KAN_SAKSBEHANDLE_DAGLIG_REISE_TSO) &&
                    unleashService.isEnabled(Toggle.KAN_SAKSBEHANDLE_DAGLIG_REISE_TSR)
                ) {
                    Stønadstype.entries
                } else {
                    Stønadstype.entries.filterNot { it.gjelderDagligReise() }
                }
            return ValgbareStønadstyperForJournalpost(
                defaultStønadstype = null,
                valgbareStønadstyper = valgbareStønadstyper,
            )
        }

        return when (dokumentbrevkode) {
            DokumentBrevkode.BARNETILSYN -> ValgbareStønadstyperForJournalpost(Stønadstype.BARNETILSYN)
            DokumentBrevkode.LÆREMIDLER -> ValgbareStønadstyperForJournalpost(Stønadstype.LÆREMIDLER)
            DokumentBrevkode.BOUTGIFTER -> ValgbareStønadstyperForJournalpost(Stønadstype.BOUTGIFTER)
            DokumentBrevkode.DAGLIG_REISE ->
                ValgbareStønadstyperForJournalpost(
                    finnStønadstypeForDagligReise(journalpost),
                    Stønadstype.entries.filter { it.gjelderDagligReise() },
                )
        }
    }

    private fun finnStønadstypeForDagligReise(journalpost: Journalpost): Stønadstype {
        if (!journalpost.harStrukturertSøknad()) {
            return if (journalpost.tema == Tema.TSO.name) {
                Stønadstype.DAGLIG_REISE_TSO
            } else {
                Stønadstype.DAGLIG_REISE_TSR
            }
        }

        // Alle daglig reise støknader legges på TSO fra fyll ut send inn
        val søknadsskjema = journalpostService.hentSøknadFraJournalpost(journalpost, Stønadstype.DAGLIG_REISE_TSO)
        val søknad = søknadService.mapSøknad(søknadsskjema, journalpost)

        if (søknad !is SøknadDagligReise) {
            error("Søknaden fra journalposten er ikke en daglig reise søknad")
        }

        val målgrupper = søknad.data.hovedytelse.hovedytelse

        // Sender til TSR hvis flere målgrupper eller TSR sine målgrupper
        return if (målgrupper.size > 1 ||
            målgrupper
                .single()
                .tilMålgruppeType()
                .kanBrukesForStønad(Stønadstype.DAGLIG_REISE_TSR)
        ) {
            Stønadstype.DAGLIG_REISE_TSR
        } else {
            Stønadstype.DAGLIG_REISE_TSO
        }
    }

    private fun håndterSøknad(
        personIdent: String,
        stønadstype: Stønadstype,
        journalpost: Journalpost,
    ) {
        if (kanAutomatiskJournalføre(personIdent, stønadstype, journalpost)) {
            journalføringService.journalførTilNyBehandling(
                journalpost = journalpost,
                personIdent = personIdent,
                stønadstype = stønadstype,
                behandlingÅrsak = BehandlingÅrsak.SØKNAD,
                oppgaveBeskrivelse = "Automatisk journalført søknad. Skal saksbehandles i ny løsning.",
                journalførendeEnhet = MASKINELL_JOURNALFOERENDE_ENHET,
            )
        } else {
            håndterSøknadSomIkkeKanAutomatiskJournalføres(
                personIdent = personIdent,
                stønadstype = stønadstype,
                journalpost = journalpost,
            )
        }
    }

    fun kanAutomatiskJournalføre(
        personIdent: String,
        stønadstype: Stønadstype,
        journalpost: Journalpost,
    ): Boolean {
        if (!journalpost.gjelderKanalNavNo()) {
            logger.info("Journalpost=${journalpost.journalpostId} kan ikke automatisk journalføres pga kanal=${journalpost.kanal}")
            return false
        }
        val allePersonIdenter = personService.hentFolkeregisterIdenter(personIdent).identer()
        val fagsak = fagsakService.finnFagsak(allePersonIdenter, stønadstype)

        return if (fagsak == null) {
            true
        } else {
            val behandlinger = behandlingService.hentBehandlinger(fagsak.id)
            !harÅpenBehandling(behandlinger)
        }
    }

    private fun harÅpenBehandling(behandlinger: List<Behandling>): Boolean = behandlinger.any { !it.erFerdigstilt() }

    private fun håndterSøknadSomIkkeKanAutomatiskJournalføres(
        personIdent: String,
        stønadstype: Stønadstype,
        journalpost: Journalpost,
    ) {
        taskService.save(
            OpprettOppgaveTask.opprettTask(
                personIdent = personIdent,
                stønadstype = stønadstype,
                oppgave =
                    OpprettOppgave(
                        oppgavetype = Oppgavetype.Journalføring,
                        beskrivelse = lagOppgavebeskrivelseForJournalføringsoppgave(journalpost, stønadstype),
                        journalpostId = journalpost.journalpostId,
                    ),
            ),
        )
    }

    private fun lagOppgavebeskrivelseForJournalføringsoppgave(
        journalpost: Journalpost,
        stønadstype: Stønadstype,
    ): String {
        // FIXME - for å håndtere at daglig-reise søknader/ettsersendelser kommer som brevkode NAV 11-12.21 og ikke NAV 11-12.21b https://nav-it.slack.com/archives/C049HPU424F/p1758780000577149
        if (journalpost.dokumentBrevkode() == DokumentBrevkode.DAGLIG_REISE && !journalpost.harStrukturertSøknad()) {
            return journalpost.førsteDokumentMedBrevkode()?.tittel ?: "Ny søknad eller ettersendelse for ${stønadstype.visningsnavn}"
        }

        if (journalpost.dokumenter.isNullOrEmpty()) error("Journalpost ${journalpost.journalpostId} mangler dokumenter")
        val dokumentTittel = journalpost.dokumenter!!.firstOrNull { it.brevkode != null }?.tittel ?: ""
        return "Må behandles i ny løsning - $dokumentTittel"
    }

    private fun Journalpost.førsteDokumentMedBrevkode() = this.dokumenter?.firstOrNull { it.brevkode != null }
}

data class ValgbareStønadstyperForJournalpost(
    val defaultStønadstype: Stønadstype?,
    val valgbareStønadstyper: List<Stønadstype>,
) {
    constructor(stønadstype: Stønadstype) : this(
        defaultStønadstype = stønadstype,
        valgbareStønadstyper = listOf(stønadstype),
    )
}
