package no.nav.tilleggsstonader.sak.ekstern.journalføring

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Skjematype
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.felles.gjelderDagligReise
import no.nav.tilleggsstonader.kontrakter.felles.gjelderReiseTilSamling
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.ytelse.ResultatKilde
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService.Companion.MASKINELL_JOURNALFOERENDE_ENHET
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.journalføring.JournalføringService
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import no.nav.tilleggsstonader.sak.journalføring.dokumentBrevkode
import no.nav.tilleggsstonader.sak.journalføring.gjelderKanalNavNo
import no.nav.tilleggsstonader.sak.journalføring.harStrukturertSøknad
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.OpprettOppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise.Reise
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadDagligReise
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.tilMålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.tilMålgruppe
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class HåndterSøknadService(
    private val journalpostService: JournalpostService,
    private val taskService: TaskService,
    private val journalføringService: JournalføringService,
    private val søknadService: SøknadService,
    private val ytelseService: YtelseService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun håndterSøknad(journalpost: Journalpost): Behandling? {
        val personIdent = journalpostService.hentIdentFraJournalpost(journalpost)
        val stønadstype =
            finnStønadstyperSomKanOpprettesFraJournalpost(journalpost).defaultStønadstype
                ?: error("Fant ikke dokument brevkode for journalpost")

        if (kanAutomatiskJournalføre(journalpost)) {
            return journalføringService.journalførTilNyBehandling(
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
            return null
        }
    }

    fun finnStønadstyperSomKanOpprettesFraJournalpost(journalpost: Journalpost): ValgbareStønadstyperForJournalpost {
        val skjematype = journalpost.dokumentBrevkode()?.tilSkjematype()

        return when (skjematype) {
            Skjematype.SØKNAD_BARNETILSYN -> ValgbareStønadstyperForJournalpost(Stønadstype.BARNETILSYN)
            Skjematype.SØKNAD_LÆREMIDLER -> ValgbareStønadstyperForJournalpost(Stønadstype.LÆREMIDLER)
            Skjematype.SØKNAD_BOUTGIFTER -> ValgbareStønadstyperForJournalpost(Stønadstype.BOUTGIFTER)
            Skjematype.SØKNAD_DAGLIG_REISE ->
                ValgbareStønadstyperForJournalpost(
                    defaultStønadstype = finnStønadstypeForDagligReise(journalpost),
                    valgbareStønadstyper = Stønadstype.entries.filter { it.gjelderDagligReise() },
                )

            Skjematype.SØKNAD_REISE_TIL_SAMLING ->
                ValgbareStønadstyperForJournalpost(
                    defaultStønadstype = finnStønadstypeForReiseTilSamling(journalpost),
                    valgbareStønadstyper = Stønadstype.entries.filter { it.gjelderReiseTilSamling() },
                )

            Skjematype.DAGLIG_REISE_KJØRELISTE ->
                error("Skal ikke behandle kjøreliste")

            null ->
                ValgbareStønadstyperForJournalpost(
                    defaultStønadstype = null,
                    valgbareStønadstyper = Stønadstype.entries,
                )
        }
    }

    private fun finnStønadstypeForReiseTilSamling(journalpost: Journalpost): Stønadstype {
        return Stønadstype.REISE_TIL_SAMLING_TSO

        // TODO: Skill ut TSO fra TSR https://favro.com/organization/98c34fb974ce445eac854de0/4d617346d79341c7fbd9a40a?card=Nav-29445
    }

    private fun finnStønadstypeForDagligReise(journalpost: Journalpost): Stønadstype {
        if (!journalpost.harStrukturertSøknad()) {
            return if (journalpost.tema == Tema.TSO.name) {
                Stønadstype.DAGLIG_REISE_TSO
            } else {
                Stønadstype.DAGLIG_REISE_TSR
            }
        }

        // Alle daglige reiser stønader legges på TSO fra fyll ut send inn
        val søknadsskjema =
            journalpostService.hentSøknadFraJournalpost(journalpost, Stønadstype.DAGLIG_REISE_TSO)
        val søknad = søknadService.mapSøknad(søknadsskjema, journalpost)

        if (søknad !is SøknadDagligReise) {
            error("Søknaden fra journalposten er ikke en daglige reiser søknad")
        }

        val målgrupperFraRegister = hentMålgrupperFraRegister(journalpost, søknad).toSet()
        val målgrupperFraSøknad =
            søknad.data.hovedytelse.hovedytelse
                .map { it.tilMålgruppeType() }
                .toSet()

        logger.info(
            "Forsøker å finne stønadstype for journalpost ${journalpost.journalpostId}, målgrupper fra register: $målgrupperFraRegister, målgrupper fra søknad: $målgrupperFraSøknad",
        )

        val målgrupper = målgrupperFraRegister.takeIf { it.isNotEmpty() } ?: målgrupperFraSøknad

        val stønadstype =
            if (målgrupper.all { it.kanBrukesForStønad(Stønadstype.DAGLIG_REISE_TSO) }) {
                Stønadstype.DAGLIG_REISE_TSO
            } else {
                Stønadstype.DAGLIG_REISE_TSR
            }

        logger.info("Stønadstype for ${journalpost.journalpostId}: $stønadstype")
        return stønadstype
    }

    private fun hentMålgrupperFraRegister(
        journalpost: Journalpost,
        søknad: SøknadDagligReise,
    ): List<MålgruppeType> {
        feilHvis(journalpost.bruker == null) {
            "Forventer at bruker skal være satt på journalpost"
        }

        return ytelseService
            .hentYtelser(
                ident = journalpost.bruker!!.id,
                fom = søknad.data.reiser.finnTidligsteDato(),
                tom = søknad.data.reiser.finnSenesteDato(),
                typer = TypeYtelsePeriode.entries.toList(),
            ).also { validerResultat(it.kildeResultat) }
            .perioder
            .map { it.type.tilMålgruppe() }
    }

    private fun validerResultat(kildeResultat: List<YtelsePerioderDto.KildeResultatYtelse>) {
        val feiledeHentingerAvYtelse = kildeResultat.filter { it.resultat == ResultatKilde.FEILET }

        feilHvis(feiledeHentingerAvYtelse.isNotEmpty()) {
            "Feil ved henting av ytelser ${feiledeHentingerAvYtelse.map { it.type }}"
        }
    }

    private fun List<Reise>.finnTidligsteDato() = flatMap { listOf(it.periode.fom, it.periode.tom) }.min()

    private fun List<Reise>.finnSenesteDato() = flatMap { listOf(it.periode.fom, it.periode.tom) }.max()

    fun kanAutomatiskJournalføre(journalpost: Journalpost): Boolean {
        if (!journalpost.gjelderKanalNavNo()) {
            logger.info("Journalpost=${journalpost.journalpostId} kan ikke automatisk journalføres pga kanal=${journalpost.kanal}")
            return false
        }
        // NAV_NO-søknader skal journalføres automatisk.
        // Hvis det finnes aktiv behandling, blir ny behandling satt på vent i OpprettBehandlingService.
        return true
    }

    private fun håndterSøknadSomIkkeKanAutomatiskJournalføres(
        personIdent: String,
        stønadstype: Stønadstype,
        journalpost: Journalpost,
    ) {
        val opprettOppgave =
            if (stønadstype.gjelderDagligReise() && !journalpost.harStrukturertSøknad()) {
                // Kommer journalposter på daglige reiser inn fra skanning før vi har tatt i bruk i prod, ønsker ikke å legge de i vår mappe
                // Kan fjernes etter daglige reiser er i prod. Se https://nav-it.slack.com/archives/C049HPU424F/p1758780000577149
                OpprettOppgave(
                    oppgavetype = Oppgavetype.Journalføring,
                    beskrivelse =
                        journalpost.førsteDokumentMedBrevkode()?.tittel
                            ?: "Ny søknad eller ettersendelse for ${stønadstype.visningsnavn}",
                    journalpostId = journalpost.journalpostId,
                    opprettIMappe = null,
                )
            } else {
                OpprettOppgave(
                    oppgavetype = Oppgavetype.Journalføring,
                    beskrivelse = lagOppgavebeskrivelseForJournalføringsoppgave(journalpost),
                    journalpostId = journalpost.journalpostId,
                )
            }

        taskService.save(
            OpprettOppgaveTask.opprettTask(
                personIdent = personIdent,
                stønadstype = stønadstype,
                oppgave = opprettOppgave,
            ),
        )
    }

    private fun lagOppgavebeskrivelseForJournalføringsoppgave(journalpost: Journalpost): String {
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
