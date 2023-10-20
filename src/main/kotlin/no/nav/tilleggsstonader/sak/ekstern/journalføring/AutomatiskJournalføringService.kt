package no.nav.tilleggsstonader.sak.ekstern.journalføring

import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgavePrioritet
import no.nav.tilleggsstonader.kontrakter.sak.journalføring.AutomatiskJournalføringResponse
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.fagsak.tilInternType
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.journalføring.JournalføringService
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.identer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AutomatiskJournalføringService(
    private val journalføringService: JournalføringService,
    private val fagsakService: FagsakService,
    private val personService: PersonService,
//    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val journalpostService: JournalpostService,
    private val behandlingService: BehandlingService,
) {

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @Transactional
    fun automatiskJournalførTilBehandling(
        journalpostId: String,
        personIdent: String,
        stønadstype: Stønadstype,
        mappeId: Long?,
        prioritet: OppgavePrioritet,
    ): Boolean {
        val journalpost = journalpostService.hentJournalpost(journalpostId)
        val fagsak = fagsakService.hentEllerOpprettFagsak(personIdent, stønadstype.tilInternType())
        val nesteBehandlingstype = utledNesteBehandlingstype(behandlingService.hentBehandlinger(fagsak.id))

        val journalførendeEnhet = "enhet-TS" // TODO:
//        val journalførendeEnhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(fagsak.hentAktivIdent())

        validerKanAutomatiskJournalføre(personIdent, stønadstype, journalpost)

        if(kanOppretteBehandling(personIdent, stønadstype)){
            journalføringService.automatiskJournalfør(
                fagsak,
                journalpost,
                journalførendeEnhet,
                mappeId,
                nesteBehandlingstype,
                prioritet,
            )
            return true
        } else {
            // Håndter søknad som ikke kan automatisk journalføres

            // Lag journalføringsoppgave
            return false
        }

    }

    fun kanOppretteBehandling(ident: String, stønadstype: Stønadstype): Boolean {
        val allePersonIdenter = personService.hentPersonIdenter(ident).identer()
        val fagsak = fagsakService.finnFagsak(allePersonIdenter, stønadstype.tilInternType())
        val behandlinger = fagsak?.let { behandlingService.hentBehandlinger(fagsak.id) } ?: emptyList()
        val behandlingstype = utledNesteBehandlingstype(behandlinger)

        return when (behandlingstype) {
            BehandlingType.FØRSTEGANGSBEHANDLING -> true
            BehandlingType.REVURDERING -> kanAutomatiskJournalføreRevurdering(behandlinger, fagsak)
        }
    }

    private fun kanAutomatiskJournalføreRevurdering(behandlinger: List<Behandling>, fagsak: Fagsak?): Boolean {
        return if (!harÅpenBehandling(behandlinger)) {
            secureLogger.info("Kan automatisk journalføre for fagsak: ${fagsak?.id}")
            true
        } else {
            secureLogger.info("Kan ikke automatisk journalføre for fagsak: ${fagsak?.id}")
            false
        }
    }

    private fun validerKanAutomatiskJournalføre(
        personIdent: String,
        stønadstype: Stønadstype,
        journalpost: Journalpost,
    ) {
        val allePersonIdenter = personService.hentPersonIdenter(personIdent).identer()

        feilHvisIkke(kanOppretteBehandling(personIdent, stønadstype)) {
            "Kan ikke opprette førstegangsbehandling for $stønadstype da det allerede finnes en behandling i infotrygd eller ny løsning"
        }

        feilHvis(journalpost.bruker == null) {
            "Journalposten mangler bruker. Kan ikke automatisk journalføre ${journalpost.journalpostId}"
        }

        journalpost.bruker?.let {
            feilHvisIkke(fagsakPersonOgJournalpostBrukerErSammePerson(allePersonIdenter, personIdent, it)) {
                "Ikke samsvar mellom personident på journalposten og personen vi forsøker å opprette behandling for. Kan ikke automatisk journalføre ${journalpost.journalpostId}"
            }
        }

        feilHvis(journalpost.journalstatus != Journalstatus.MOTTATT) {
            "Journalposten har ugyldig journalstatus ${journalpost.journalstatus}. Kan ikke automatisk journalføre ${journalpost.journalpostId}"
        }
    }

    private fun fagsakPersonOgJournalpostBrukerErSammePerson(
        allePersonIdenter: Set<String>,
        gjeldendePersonIdent: String,
        journalpostBruker: Bruker,
    ): Boolean = when (journalpostBruker.type) {
        BrukerIdType.FNR -> allePersonIdenter.contains(journalpostBruker.id)
        BrukerIdType.AKTOERID -> hentAktørIderForPerson(gjeldendePersonIdent).contains(journalpostBruker.id)
        BrukerIdType.ORGNR -> false
    }

    private fun hentAktørIderForPerson(personIdent: String) =
        personService.hentAktørIder(personIdent).identer()

    private fun utledNesteBehandlingstype(behandlinger: List<Behandling>): BehandlingType {
        return if (behandlinger.all { it.resultat == BehandlingResultat.HENLAGT }) BehandlingType.FØRSTEGANGSBEHANDLING else BehandlingType.REVURDERING
    }

    private fun harÅpenBehandling(behandlinger: List<Behandling>): Boolean {
        return behandlinger.any { !it.erAvsluttet() }
    }
}
