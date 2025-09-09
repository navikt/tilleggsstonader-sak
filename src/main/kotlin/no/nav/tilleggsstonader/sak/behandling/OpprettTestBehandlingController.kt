package no.nav.tilleggsstonader.sak.behandling

import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
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
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaBoutgifterFyllUtSendInn
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaDagligReiseFyllUtSendInn
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaLæremidler
import no.nav.tilleggsstonader.kontrakter.søknad.TekstFelt
import no.nav.tilleggsstonader.kontrakter.søknad.VerdiFelt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.AktivitetAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.AnnenAktivitetType
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.BarnAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.BarnMedBarnepass
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.TypeBarnepass
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.Aktivitet
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.Aktiviteter
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.AktiviteterOgMålgruppe
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.ArbeidsrettetAktivitetType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.ArsakOppholdUtenforNorgeType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.BoligEllerOvernatting
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.BoutgifterFyllUtSendInnData
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.DelerBoutgifterType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.DineOpplysninger
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.FasteUtgifter
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.HarPengestotteAnnetLandType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.HarUtgifterTilBoligToStederType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.HovedytelseType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.Identitet
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.JaNeiType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.Landvelger
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.NavAdresse
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.Periode
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.PeriodeForSamling
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.Samling
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.SkjemaBoutgifter
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.TypeUtgifterType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.UtgifterFlereSteder
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.UtgifterNyBolig
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HvaErViktigsteGrunnerTilAtDuIkkeKanBrukeOffentligTransportType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HvaSlagsTypeBillettMaDuKjopeType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.PeriodeAktivitet
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.SkjemaDagligReise
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Valgfelt
import no.nav.tilleggsstonader.kontrakter.søknad.felles.ArbeidOgOpphold
import no.nav.tilleggsstonader.kontrakter.søknad.felles.HovedytelseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.felles.OppholdUtenforNorge
import no.nav.tilleggsstonader.kontrakter.søknad.felles.TypePengestøtte
import no.nav.tilleggsstonader.kontrakter.søknad.felles.ÅrsakOppholdUtenforNorge
import no.nav.tilleggsstonader.kontrakter.søknad.læremidler.AnnenUtdanningType
import no.nav.tilleggsstonader.kontrakter.søknad.læremidler.HarRettTilUtstyrsstipend
import no.nav.tilleggsstonader.kontrakter.søknad.læremidler.UtdanningAvsnitt
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.context.annotation.Profile
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.ArbeidOgOpphold as ArbeidOgOppholdBoutgifter
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.OppholdUtenforNorge as OppholdUtenforNorgeBoutgifter
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Aktivitet as AktivitetDagligReise
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Aktiviteter as AktiviteterDagligReise
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.AktiviteterOgMålgruppe as AktiviteterOgMålgruppeDagligReise
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.ArbeidOgOpphold as ArbeidOgOppholdDagligReise
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.ArsakOppholdUtenforNorgeType as ArsakOppholdUtenforNorgeTypeDagligReise
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.DagligReiseFyllUtSendInnData as DagligReiseFyllUtSendInnData
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.DineOpplysninger as DineOpplysningerDagligReise
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HarPengestotteAnnetLandType as HarPengestotteAnnetLandTypeDagligReise
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HovedytelseType as HovedytelseTypeDagligReise
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Identitet as IdentitetDagligReise
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.JaNeiType as JaNeiTypeDagligReise
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Landvelger as LandvelgerDagligReise
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.NavAdresse as NavAdresseDagligReise
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.OppholdUtenforNorge as OppholdUtenforNorgeDagligReise
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Periode as PeriodeDagligReise
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Reise as ReiseDagligReise
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.ReiseAdresse as ReiseAdresseDagligReise
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Reiseperiode as ReiseperiodeDagligReise

@RestController
@RequestMapping(path = ["/api/test/opprett-behandling"])
@ProtectedWithClaims(issuer = "azuread")
@Profile("!prod")
class OpprettTestBehandlingController(
    private val tilgangService: TilgangService,
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val personService: PersonService,
    private val barnService: BarnService,
    private val søknadService: SøknadService,
    private val taskService: TaskService,
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
        opprettOppgave(behandling)

        return behandling.id
    }

    private fun lagBehandling(fagsak: Fagsak): Behandling =
        behandlingService.opprettBehandling(
            fagsakId = fagsak.id,
            behandlingsårsak = BehandlingÅrsak.SØKNAD,
        )

    private fun opprettSøknad(
        fagsak: Fagsak,
        behandling: Behandling,
    ) {
        when (fagsak.stønadstype) {
            Stønadstype.BARNETILSYN -> opprettSøknadBarnetilsyn(fagsak, behandling)
            Stønadstype.LÆREMIDLER -> opprettSøknadLæremidler(fagsak, behandling)
            Stønadstype.BOUTGIFTER -> opprettSøknadBoutgifter(fagsak, behandling)
            Stønadstype.DAGLIG_REISE_TSO -> opprettSøknadDagligeReise(fagsak, behandling)
            Stønadstype.DAGLIG_REISE_TSR -> opprettSøknadDagligeReise(fagsak, behandling)
        }
    }

    private fun opprettSøknadBarnetilsyn(
        fagsak: Fagsak,
        behandling: Behandling,
    ) {
        val pdlBarn = personService.hentPersonMedBarn(fagsak.hentAktivIdent()).barn
        val barnMedBarnepass =
            pdlBarn.entries.map { (ident, _) ->
                BarnMedBarnepass(
                    ident = TekstFelt("", ident),
                    navn = TekstFelt("", "navn"),
                    type = EnumFelt("", TypeBarnepass.BARNEHAGE_SFO_AKS, "", emptyList()),
                    startetIFemte = null,
                    årsak = null,
                )
            }
        val skjemaBarnetilsyn =
            SøknadsskjemaBarnetilsyn(
                hovedytelse =
                    HovedytelseAvsnitt(
                        hovedytelse = EnumFlereValgFelt("", listOf(VerdiFelt(Hovedytelse.AAP, "AAP")), emptyList()),
                        arbeidOgOpphold = arbeidOgOpphold(),
                    ),
                aktivitet =
                    AktivitetAvsnitt(
                        aktiviteter =
                            EnumFlereValgFelt(
                                "Hvilken aktivitet søker du om støtte i forbindelse med?",
                                listOf(
                                    VerdiFelt("ANNET", "Annet"),
                                    VerdiFelt("1", "Arbeidstrening: 25. februar 2024 - 25. juli 2024"),
                                ),
                                listOf("Arbeidstrening: 25. februar 2024 - 25. juli 2024"),
                            ),
                        annenAktivitet =
                            EnumFelt(
                                "Hvilken arbeidsrettet aktivitet har du? ",
                                AnnenAktivitetType.TILTAK,
                                "Tiltak / arbeidsrettet aktivitet",
                                listOf(),
                            ),
                        lønnetAktivitet = EnumFelt("Mottar du lønn gjennom ett tiltak?", JaNei.NEI, "Nei", listOf()),
                    ),
                barn =
                    BarnAvsnitt(
                        barnMedBarnepass = barnMedBarnepass,
                    ),
                dokumentasjon = emptyList(),
            )
        val skjema =
            Søknadsskjema(
                ident = fagsak.hentAktivIdent(),
                mottattTidspunkt = LocalDateTime.now(),
                språk = Språkkode.NB,
                skjema = skjemaBarnetilsyn,
            )
        val journalpost = Journalpost("TESTJPID", Journalposttype.I, Journalstatus.FERDIGSTILT)
        val søknad = søknadService.lagreSøknad(behandling.id, journalpost, skjema)
        opprettBarn(behandling, søknad as SøknadBarnetilsyn)
    }

    private fun opprettSøknadLæremidler(
        fagsak: Fagsak,
        behandling: Behandling,
    ) {
        val skjemaLæremidler =
            SøknadsskjemaLæremidler(
                hovedytelse =
                    HovedytelseAvsnitt(
                        hovedytelse = EnumFlereValgFelt("", listOf(VerdiFelt(Hovedytelse.AAP, "AAP")), emptyList()),
                        arbeidOgOpphold = arbeidOgOpphold(),
                    ),
                utdanning =
                    UtdanningAvsnitt(
                        aktiviteter =
                            EnumFlereValgFelt(
                                "Hvilken utdanning eller opplæring søker du om støtte til læremidler for",
                                listOf(
                                    VerdiFelt("1", "Høyere utdanning: 25. februar 2024 - 25. juli 2024"),
                                ),
                                listOf("Arbeidstrening: 25. februar 2024 - 25. juli 2024"),
                            ),
                        annenUtdanning =
                            EnumFelt(
                                "Annen utdanning tekst",
                                AnnenUtdanningType.INGEN_UTDANNING,
                                "Ja",
                                emptyList(),
                            ),
                        harRettTilUtstyrsstipend =
                            HarRettTilUtstyrsstipend(
                                erLærlingEllerLiknende =
                                    EnumFelt(
                                        "Er lærling eller liknende?",
                                        JaNei.JA,
                                        "Ja",
                                        emptyList(),
                                    ),
                                harTidligereFullførtVgs =
                                    EnumFelt(
                                        "Har du tidligere fullført videregående skole?",
                                        JaNei.JA,
                                        "Ja",
                                        emptyList(),
                                    ),
                            ),
                        harFunksjonsnedsettelse = EnumFelt("Har funksjonsnedsettelse?", JaNei.JA, "Ja", emptyList()),
                    ),
                dokumentasjon = emptyList(),
            )
        val skjema =
            Søknadsskjema(
                ident = fagsak.hentAktivIdent(),
                mottattTidspunkt = LocalDateTime.of(2020, 1, 1, 0, 0),
                språk = Språkkode.NB,
                skjema = skjemaLæremidler,
            )
        val journalpost = Journalpost("TESTJPID", Journalposttype.I, Journalstatus.FERDIGSTILT)
        søknadService.lagreSøknad(behandling.id, journalpost, skjema)
    }

    private fun opprettSøknadBoutgifter(
        fagsak: Fagsak,
        behandling: Behandling,
    ) {
        val dineOpplysninger =
            DineOpplysninger(
                fornavn = "Fornavn",
                etternavn = "Etternavn",
                identitet = Identitet(identitetsnummer = "11111122222"),
                adresse =
                    NavAdresse(
                        gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                        adresse = "Nisseveien 3",
                        postnummer = "0011",
                        bySted = "OSLO",
                        landkode = "NO",
                        land = Landvelger(value = "Norge", label = "NO"),
                    ),
            )
        val aktiviteter =
            Aktiviteter(
                aktiviteterOgMaalgruppe =
                    AktiviteterOgMålgruppe(
                        aktivitet =
                            Aktivitet(
                                aktivitetId = "ingenAktivitet",
                                text = "",
                                periode = null,
                                maalgruppe = null,
                            ),
                    ),
                arbeidsrettetAktivitet = ArbeidsrettetAktivitetType.tiltakArbeidsrettetUtredning,
                mottarLonnGjennomTiltak = JaNeiType.nei,
            )
        val periodeForSamling =
            PeriodeForSamling(
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 1, 1),
                trengteEkstraOvernatting = JaNeiType.nei,
                utgifterTilOvernatting = 1000,
            )
        val periodeForSamling2 =
            PeriodeForSamling(
                fom = LocalDate.of(2025, 2, 1),
                tom = LocalDate.of(2025, 2, 1),
                trengteEkstraOvernatting = JaNeiType.ja,
                utgifterTilOvernatting = 1000,
            )
        val boligEllerOvernatting =
            BoligEllerOvernatting(
                typeUtgifter = TypeUtgifterType.fastUtgift,
                fasteUtgifter =
                    FasteUtgifter(
                        harUtgifterTilBoligToSteder = HarUtgifterTilBoligToStederType.ekstraBolig,
                        utgifterFlereSteder =
                            UtgifterFlereSteder(
                                delerBoutgifter = mapOf(DelerBoutgifterType.aktivitetssted to true),
                                andelUtgifterBoligHjemsted = 1300,
                                andelUtgifterBoligAktivitetssted = 1000,
                            ),
                        utgifterNyBolig =
                            UtgifterNyBolig(
                                delerBoutgifter = JaNeiType.ja,
                                andelUtgifterBolig = 900,
                                harHoyereUtgifterPaNyttBosted = JaNeiType.ja,
                                mottarBostotte = JaNeiType.nei,
                            ),
                    ),
                samling = Samling(periodeForSamling = listOf(periodeForSamling, periodeForSamling2)),
                harSaerligStoreUtgifterPaGrunnAvFunksjonsnedsettelse = JaNeiType.nei,
            )
        val skjemaBoutgifter =
            SøknadsskjemaBoutgifterFyllUtSendInn(
                language = "nb-NO",
                data =
                    BoutgifterFyllUtSendInnData(
                        SkjemaBoutgifter(
                            dineOpplysninger = dineOpplysninger,
                            hovedytelse = mapOf(HovedytelseType.arbeidsavklaringspenger to true),
                            harNedsattArbeidsevne = JaNeiType.ja,
                            arbeidOgOpphold = arbeidOgOppholdBoutgifter(),
                            aktiviteter = aktiviteter,
                            boligEllerOvernatting = boligEllerOvernatting,
                        ),
                    ),
                dokumentasjon = emptyList(),
            )

        val skjema =
            Søknadsskjema(
                ident = fagsak.hentAktivIdent(),
                mottattTidspunkt = LocalDateTime.now(),
                språk = Språkkode.NB,
                skjema = skjemaBoutgifter,
            )
        val journalpost =
            Journalpost(
                "TESTJPID",
                Journalposttype.I,
                Journalstatus.FERDIGSTILT,
                dokumenter =
                    listOf(
                        DokumentInfo(
                            tittel = "Søknad om boutgifter",
                            dokumentInfoId = "dokumentInfoId",
                            brevkode = "BOUTGIFTER",
                        ),
                    ),
            )
        søknadService.lagreSøknad(behandling.id, journalpost, skjema)
    }

    private fun opprettSøknadDagligeReise(
        fagsak: Fagsak,
        behandling: Behandling,
    ) {
        val dineOpplysninger =
            DineOpplysningerDagligReise(
                fornavn = "Fornavn",
                etternavn = "Etternavn",
                identitet =
                    IdentitetDagligReise(identitetsnummer = "11111122222"),
                adresse =
                    NavAdresseDagligReise(
                        gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                        adresse = "Nisseveien 3",
                        postnummer = "0011",
                        bySted = "OSLO",
                        landkode = "NO",
                        land =
                            LandvelgerDagligReise(
                                value = "Norge",
                                label = "NO",
                            ),
                    ),
                reiseFraAnnetEnnFolkeregistrertAdr = JaNeiTypeDagligReise.nei,
                adresseJegSkalReiseFra = null,
            )
        val aktiviteter =
            AktiviteterDagligReise(
                aktiviteterOgMaalgruppe =
                    AktiviteterOgMålgruppeDagligReise(
                        aktivitet =
                            AktivitetDagligReise(
                                aktivitetId = "ingenAktivitet",
                                text = "AAP",
                                periode = PeriodeAktivitet(LocalDate.of(2025, 5, 20), LocalDate.of(2025, 7, 20)),
                                maalgruppe = null,
                            ),
                    ),
                arbeidsrettetAktivitet = null,
                mottarLonnGjennomTiltak = JaNeiTypeDagligReise.ja,
                reiseTilAktivitetsstedHelePerioden = JaNeiTypeDagligReise.ja,
                reiseperiode =
                    ReiseperiodeDagligReise(
                        LocalDate.of(2025, 6, 20),
                        LocalDate.of(2025, 5, 20),
                    ),
            )
        val reise =
            ReiseDagligReise(
                reiseAdresse =
                    ReiseAdresseDagligReise(gateadresse = "Nisseveien 3", postnr = "0011", poststed = "OSLO"),
                hvorMangeDagerIUkenSkalDuMoteOppPaAktivitetstedet = Valgfelt("dager", "5"),
                harDu6KmReisevei = JaNeiTypeDagligReise.ja,
                hvorLangErReiseveienDin = 8,
                harDuAvMedisinskeArsakerBehovForTransportUavhengigAvReisensLengde = JaNeiTypeDagligReise.nei,
                kanDuReiseMedOffentligTransport = JaNeiTypeDagligReise.ja,
                hvaSlagsTypeBillettMaDuKjope = mapOf(HvaSlagsTypeBillettMaDuKjopeType.enkeltbillett to true),
                enkeltbilett = 1,
                syvdagersbilett = 0,
                manedskort = 0,
                hvaErViktigsteGrunnerTilAtDuIkkeKanBrukeOffentligTransport =
                    mapOf(HvaErViktigsteGrunnerTilAtDuIkkeKanBrukeOffentligTransportType.annet to false),
                kanDuKjoreMedEgenBil = JaNeiTypeDagligReise.nei,
                utgifterBil = null,
                drosje = null,
            )

        val skjemaDagligReise =
            SøknadsskjemaDagligReiseFyllUtSendInn(
                language = "nb-NO",
                data =
                    DagligReiseFyllUtSendInnData(
                        SkjemaDagligReise(
                            dineOpplysninger = dineOpplysninger,
                            hovedytelse =
                                mapOf(
                                    HovedytelseTypeDagligReise.arbeidsavklaringspenger to
                                        true,
                                ),
                            arbeidOgOpphold = arbeidOgOppholdDagligReise(),
                            aktiviteter = aktiviteter,
                            reise = listOf(reise),
                        ),
                    ),
                dokumentasjon = emptyList(),
            )

        val skjema =
            Søknadsskjema(
                ident = fagsak.hentAktivIdent(),
                mottattTidspunkt = LocalDateTime.now(),
                språk = Språkkode.NB,
                skjema = skjemaDagligReise,
            )
        val (tittel, brevkode) =
            when (fagsak.stønadstype) {
                Stønadstype.DAGLIG_REISE_TSO ->
                    "Søknad om Daglig reise tso" to "DAGLIG_REISE_TSO"
                Stønadstype.DAGLIG_REISE_TSR ->
                    "Søknad om Daglig reise tsr" to "DAGLIG_REISE_TSR"
                else -> error("Ugyldig stønadstype for daglig reise: ${fagsak.stønadstype}")
            }

        val journalpost =
            Journalpost(
                "TESTJPID",
                Journalposttype.I,
                Journalstatus.FERDIGSTILT,
                dokumenter =
                    listOf(
                        DokumentInfo(
                            tittel = tittel,
                            dokumentInfoId = "dokumentInfoId",
                            brevkode = brevkode,
                        ),
                    ),
            )
        søknadService.lagreSøknad(behandling.id, journalpost, skjema)
    }

    private fun arbeidOgOpphold() =
        ArbeidOgOpphold(
            jobberIAnnetLand = EnumFelt("Jobber du i et annet land enn Norge?", JaNei.JA, "Ja", emptyList()),
            jobbAnnetLand = SelectFelt("Hvilket land jobber du i?", "SWE", "Sverige"),
            harPengestøtteAnnetLand =
                EnumFlereValgFelt(
                    "Mottar du pengestøtte fra et annet land enn Norge?",
                    listOf(
                        VerdiFelt(
                            TypePengestøtte.SYKEPENGER,
                            "Sykepenger",
                        ),
                    ),
                    emptyList(),
                ),
            pengestøtteAnnetLand = SelectFelt("Hvilket land mottar du pengestøtte fra?", "SWE", "Sverige"),
            harOppholdUtenforNorgeSiste12mnd =
                EnumFelt(
                    "Jobber du i et annet land enn Norge?",
                    JaNei.JA,
                    "Ja",
                    emptyList(),
                ),
            oppholdUtenforNorgeSiste12mnd = listOf(oppholdUtenforNorge()),
            harOppholdUtenforNorgeNeste12mnd =
                EnumFelt(
                    "Jobber du i et annet land enn Norge?",
                    JaNei.JA,
                    "Ja",
                    emptyList(),
                ),
            oppholdUtenforNorgeNeste12mnd = listOf(oppholdUtenforNorge()),
        )

    private fun arbeidOgOppholdBoutgifter() =
        ArbeidOgOppholdBoutgifter(
            jobberIAnnetLand = JaNeiType.ja,
            jobbAnnetLand = Landvelger("SWE", "Sverige"),
            harPengestotteAnnetLand = mapOf(HarPengestotteAnnetLandType.sykepenger to true),
            pengestotteAnnetLand = Landvelger("SWE", "Sverige"),
            harOppholdUtenforNorgeSiste12mnd = JaNeiType.ja,
            oppholdUtenforNorgeSiste12mnd =
                OppholdUtenforNorgeBoutgifter(
                    Landvelger("SWE", "Sverige"),
                    mapOf(ArsakOppholdUtenforNorgeType.besokteFamilie to true),
                    Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)),
                ),
            harOppholdUtenforNorgeNeste12mnd = JaNeiType.ja,
            oppholdUtenforNorgeNeste12mnd =
                OppholdUtenforNorgeBoutgifter(
                    Landvelger("SWE", "Sverige"),
                    mapOf(ArsakOppholdUtenforNorgeType.besokteFamilie to true),
                    Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)),
                ),
        )

    private fun arbeidOgOppholdDagligReise() =
        ArbeidOgOppholdDagligReise(
            jobberIAnnetLand = JaNeiTypeDagligReise.ja,
            jobbAnnetLand =
                LandvelgerDagligReise("SWE", "Sverige"),
            harPengestotteAnnetLand =
                mapOf(
                    HarPengestotteAnnetLandTypeDagligReise.sykepenger to true,
                ),
            pengestotteAnnetLand =
                LandvelgerDagligReise("SWE", "Sverige"),
            harOppholdUtenforNorgeSiste12mnd = JaNeiTypeDagligReise.ja,
            oppholdUtenforNorgeSiste12mnd =
                OppholdUtenforNorgeDagligReise(
                    LandvelgerDagligReise("SWE", "Sverige"),
                    mapOf(
                        ArsakOppholdUtenforNorgeTypeDagligReise.besokteFamilie to true,
                    ),
                    PeriodeDagligReise(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)),
                ),
            harOppholdUtenforNorgeNeste12mnd = JaNeiTypeDagligReise.ja,
            oppholdUtenforNorgeNeste12mnd =
                OppholdUtenforNorgeDagligReise(
                    LandvelgerDagligReise("SWE", "Sverige"),
                    mapOf(
                        ArsakOppholdUtenforNorgeTypeDagligReise.besokteFamilie to true,
                    ),
                    PeriodeDagligReise(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)),
                ),
        )

    private fun oppholdUtenforNorge() =
        OppholdUtenforNorge(
            land = SelectFelt("Hvilket land har du oppholdt deg i?", "SWE", "Sverige"),
            årsak =
                EnumFlereValgFelt(
                    "Hva gjorde du i dette landet?",
                    listOf(VerdiFelt(ÅrsakOppholdUtenforNorge.JOBB, "Jobb")),
                    alternativer = emptyList(),
                ),
            fom = DatoFelt("Fom", LocalDate.of(2024, 1, 1)),
            tom = DatoFelt("Fom", LocalDate.of(2024, 1, 1)),
        )

    // Oppretter BehandlingBarn for alle barn fra PDL for å få et vilkår per barn
    private fun opprettBarn(
        behandling: Behandling,
        søknad: SøknadBarnetilsyn,
    ) {
        val behandlingBarn =
            søknad.barn.map { barn ->
                BehandlingBarn(
                    behandlingId = behandling.id,
                    ident = barn.ident,
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
        fagsakService.hentEllerOpprettFagsak(testBehandlingRequest.personIdent, testBehandlingRequest.stønadstype)
}

data class TestBehandlingRequest(
    val personIdent: String,
    val stønadstype: Stønadstype,
)
