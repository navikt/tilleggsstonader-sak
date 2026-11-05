package no.nav.tilleggsstonader.sak.ekstern.journalføring

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Skjematype
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.felles.gjelderDagligReise
import no.nav.tilleggsstonader.kontrakter.felles.gjelderFlytting
import no.nav.tilleggsstonader.kontrakter.felles.gjelderReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.kontrakter.felles.gjelderReiseTilSamling
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService.Companion.MASKINELL_JOURNALFOERENDE_ENHET
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.journalføring.JournalføringService
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import no.nav.tilleggsstonader.sak.journalføring.dokumentBrevkode
import no.nav.tilleggsstonader.sak.journalføring.gjelderKanalNavNo
import no.nav.tilleggsstonader.sak.journalføring.harStrukturertSøknad
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.OpprettOppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise.Reise
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Søknad
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadDagligReise
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadFlytting
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadROAH
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadReiseTilSamling
import no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling.Samling
import no.nav.tilleggsstonader.sak.opplysninger.søknad.roah.ROAHReise
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.tilMålgruppeType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class HåndterSøknadService(
    private val journalpostService: JournalpostService,
    private val taskService: TaskService,
    private val journalføringService: JournalføringService,
    private val søknadService: SøknadService,
    private val unleashService: UnleashService,
    private val bestemTemaForJournalpostService: BestemTemaForJournalpostService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun håndterSøknad(journalpost: Journalpost): Behandling? {
        val personIdent = journalpostService.hentIdentFraJournalpost(journalpost)
        val stønadstype =
            finnStønadstyperSomKanOpprettesFraJournalpost(journalpost, filtrerStønadstyperSomIkkeErAktivert = false).defaultStønadstype
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

    /**
     * OBS - ved nye stønadstyper bør "valgbareStønadstyperForIkkeStøttetSkjematype()" returneres inntil det er prod-klart,
     * hvis ikke er det mulig for saksbehandler å opprette saker av denne typen i prod.
     *
     * @param filtrerStønadstyperSomIkkeErAktivert - hvis true, returnerer ikke stønadsdtypen som tilhører brevkoden
     * hvis ikke den er skrudd på i prod
     */
    fun finnStønadstyperSomKanOpprettesFraJournalpost(
        journalpost: Journalpost,
        filtrerStønadstyperSomIkkeErAktivert: Boolean,
    ): ValgbareStønadstyperForJournalpost {
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
                if (unleashService.isEnabled(Toggle.KAN_BEHANDLE_REISE_TIL_SAMLING) || !filtrerStønadstyperSomIkkeErAktivert) {
                    ValgbareStønadstyperForJournalpost(
                        defaultStønadstype = finnStønadstypeForReiseTilSamling(journalpost),
                        valgbareStønadstyper = Stønadstype.entries.filter { it.gjelderReiseTilSamling() },
                    )
                } else {
                    // Saksbehandler skal kunne velge en annen stønadstype hvis bruker har brukt feil søknadsskjema
                    valgbareStønadstyperForIkkeStøttetSkjematype()
                }

            Skjematype.SØKNAD_FLYTTING ->
                if (unleashService.isEnabled(Toggle.KAN_BEHANDLE_FLYTTING) || !filtrerStønadstyperSomIkkeErAktivert) {
                    ValgbareStønadstyperForJournalpost(
                        defaultStønadstype = finnStønadstypeForFlytting(journalpost),
                        valgbareStønadstyper = Stønadstype.entries.filter { it.gjelderFlytting() },
                    )
                } else {
                    // Saksbehandler skal kunne velge en annen stønadstype hvis bruker har brukt feil søknadsskjema
                    valgbareStønadstyperForIkkeStøttetSkjematype()
                }

            Skjematype.SØKNAD_REISE_OPPSTART_AVSLUTNING_HJEMREISE ->
                if (unleashService.isEnabled(Toggle.KAN_BEHANDLE_REISE_OPPSTART_AVSLUTNING_HJEMREISE) ||
                    !filtrerStønadstyperSomIkkeErAktivert
                ) {
                    ValgbareStønadstyperForJournalpost(
                        defaultStønadstype = finnStønadstypeForReiseOppstartAvslutningHjemreise(journalpost),
                        valgbareStønadstyper = Stønadstype.entries.filter { it.gjelderReiseOppstartAvslutningHjemreise() },
                    )
                } else {
                    // Saksbehandler skal kunne velge en annen stønadstype hvis bruker har brukt feil søknadsskjema
                    valgbareStønadstyperForIkkeStøttetSkjematype()
                }

            Skjematype.DAGLIG_REISE_KJØRELISTE ->
                error("Skal ikke behandle kjøreliste")

            null -> valgbareStønadstyperForIkkeStøttetSkjematype()
        }
    }

    private fun valgbareStønadstyperForIkkeStøttetSkjematype() =
        ValgbareStønadstyperForJournalpost(
            defaultStønadstype = null,
            valgbareStønadstyper = finnTilgjengeligeStønadstyper(),
        )

    /**
     * Stønadstyper som ikke kan opprettes automatisk fra en journalpost, f.eks. fordi de foreløpig ikke
     * er togglet på i prod, skal heller ikke være valgbare når saksbehandler skal velge stønadstype manuelt.
     */
    private fun finnTilgjengeligeStønadstyper(): List<Stønadstype> {
        val utilgjengeligeStønadstyper = mutableSetOf<Stønadstype>()

        if (!unleashService.isEnabled(Toggle.KAN_BEHANDLE_REISE_TIL_SAMLING)) {
            utilgjengeligeStønadstyper.addAll(Stønadstype.entries.filter { it.gjelderReiseTilSamling() })
        }
        if (!unleashService.isEnabled(Toggle.KAN_BEHANDLE_FLYTTING)) {
            utilgjengeligeStønadstyper.addAll(Stønadstype.entries.filter { it.gjelderFlytting() })
        }
        if (!unleashService.isEnabled(Toggle.KAN_BEHANDLE_REISE_OPPSTART_AVSLUTNING_HJEMREISE)) {
            utilgjengeligeStønadstyper.addAll(Stønadstype.entries.filter { it.gjelderReiseOppstartAvslutningHjemreise() })
        }

        return Stønadstype.entries - utilgjengeligeStønadstyper
    }

    private fun finnStønadstypeForReiseTilSamling(journalpost: Journalpost) =
        bestemStønadstype(
            journalpost = journalpost,
            stønadstypeTso = Stønadstype.REISE_TIL_SAMLING_TSO,
            StønadstypeTsr = Stønadstype.REISE_TIL_SAMLING_TSR,
        )

    private fun finnStønadstypeForFlytting(journalpost: Journalpost) =
        bestemStønadstype(
            journalpost = journalpost,
            stønadstypeTso = Stønadstype.FLYTTING_TSO,
            StønadstypeTsr = Stønadstype.FLYTTING_TSR,
        )

    private fun finnStønadstypeForReiseOppstartAvslutningHjemreise(journalpost: Journalpost) =
        bestemStønadstype(
            journalpost = journalpost,
            stønadstypeTso = Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO,
            StønadstypeTsr = Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR,
        )

    private fun finnStønadstypeForDagligReise(journalpost: Journalpost) =
        bestemStønadstype(
            journalpost = journalpost,
            stønadstypeTso = Stønadstype.DAGLIG_REISE_TSO,
            StønadstypeTsr = Stønadstype.DAGLIG_REISE_TSR,
        )

    private fun bestemStønadstype(
        journalpost: Journalpost,
        stønadstypeTso: Stønadstype,
        StønadstypeTsr: Stønadstype,
    ): Stønadstype {
        if (!journalpost.harStrukturertSøknad()) {
            return if (journalpost.tema == Tema.TSO.name) {
                stønadstypeTso
            } else {
                StønadstypeTsr
            }
        }

        val søknadsskjema =
            journalpostService.hentSøknadFraJournalpost(journalpost, stønadstypeTso)
        val søknad = søknadService.mapSøknad(søknadsskjema, journalpost)
        val periode = finnPeriodeDetSkalSjekkesMotYtelser(søknad)

        return bestemTemaForJournalpostService.bestemStønadstype(
            journalpost = journalpost,
            stønadstypeTso = stønadstypeTso,
            stønadstypeTsr = StønadstypeTsr,
            fom = periode.fom,
            tom = periode.tom,
            målgrupperFraSøknad =
                søknad.data.hovedytelse.hovedytelse
                    .map { it.tilMålgruppeType() }
                    .toSet(),
        )
    }

    private fun finnPeriodeDetSkalSjekkesMotYtelser(søknad: Søknad<*>) =
        when (søknad) {
            is SøknadFlytting -> {
                val flyttedato = søknad.data.flyttingDetaljer?.flyttedato ?: feil("Mangler flyttedetaljer i flyttingsøknad")
                Datoperiode(fom = flyttedato, tom = flyttedato)
            }
            is SøknadROAH ->
                Datoperiode(
                    fom = søknad.data.reiser.finnTidligsteDatoROAH(),
                    tom = søknad.data.reiser.finnSenesteDatoROAH(),
                )
            is SøknadDagligReise ->
                Datoperiode(
                    fom = søknad.data.reiser.finnTidligsteDato(),
                    tom = søknad.data.reiser.finnSenesteDato(),
                )
            is SøknadReiseTilSamling ->
                Datoperiode(
                    fom = søknad.data.samlinger.finnTidligsteDatoForSamling(),
                    tom = søknad.data.samlinger.finnSenesteDatoForSamling(),
                )
            else -> error("Uforventet søknad-type ${søknad.javaClass.simpleName}")
        }

    private fun List<Reise>.finnTidligsteDato() = flatMap { listOf(it.periode.fom, it.periode.tom) }.min()

    private fun List<Reise>.finnSenesteDato() = flatMap { listOf(it.periode.fom, it.periode.tom) }.max()

    private fun List<ROAHReise>.finnTidligsteDatoROAH() =
        flatMap { listOfNotNull(it.fom, it.tom) }.minOrNull()
            ?: error("Finner ingen reisedatoer i søknaden")

    private fun List<ROAHReise>.finnSenesteDatoROAH() =
        flatMap { listOfNotNull(it.fom, it.tom) }.maxOrNull()
            ?: error("Finner ingen reisedatoer i søknaden")

    private fun List<Samling>.finnTidligsteDatoForSamling() = flatMap { listOf(it.fom, it.tom) }.min()

    private fun List<Samling>.finnSenesteDatoForSamling() = flatMap { listOf(it.fom, it.tom) }.max()

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
            if (!erSaksbehandlingSkruddPåForStønadstype(stønadstype) && !journalpost.harStrukturertSøknad()) {
                // Kommer journalposter inn fra skanning på stønadstyper vi ikke har prodsatt, ønsker ikke å legge de i vår mappe
                // Kan fjernes etter vi støtter alle tilleggsstønader
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

    private fun erSaksbehandlingSkruddPåForStønadstype(stønadstype: Stønadstype): Boolean =
        when (stønadstype) {
            Stønadstype.BARNETILSYN,
            Stønadstype.LÆREMIDLER,
            Stønadstype.BOUTGIFTER,
            Stønadstype.DAGLIG_REISE_TSO,
            Stønadstype.DAGLIG_REISE_TSR,
            -> true
            Stønadstype.REISE_TIL_SAMLING_TSO,
            Stønadstype.REISE_TIL_SAMLING_TSR,
            ->
                unleashService.isEnabled(Toggle.KAN_BEHANDLE_REISE_TIL_SAMLING)
            Stønadstype.FLYTTING_TSO,
            Stønadstype.FLYTTING_TSR,
            ->
                unleashService.isEnabled(Toggle.KAN_BEHANDLE_FLYTTING)
            Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO,
            Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR,
            ->
                unleashService.isEnabled(Toggle.KAN_BEHANDLE_REISE_OPPSTART_AVSLUTNING_HJEMREISE)
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
