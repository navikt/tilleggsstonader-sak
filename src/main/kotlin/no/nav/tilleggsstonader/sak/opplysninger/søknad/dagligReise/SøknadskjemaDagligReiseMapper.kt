package no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaDagligReiseFyllUtSendInn
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.AnnenAktivitetType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Aktiviteter
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.ArbeidsrettetAktivitetType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.ArsakOppholdUtenforNorgeType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.DineOpplysninger
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HarPengestotteAnnetLandType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HovedytelseType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HvaErViktigsteGrunnerTilAtDuIkkeKanBrukeOffentligTransportType.annet
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HvaErViktigsteGrunnerTilAtDuIkkeKanBrukeOffentligTransportType.darligTransporttilbud
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HvaErViktigsteGrunnerTilAtDuIkkeKanBrukeOffentligTransportType.helsemessigeArsaker
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HvaErViktigsteGrunnerTilAtDuIkkeKanBrukeOffentligTransportType.leveringHentingIBarnehageEllerSkolefritidsordningSfoAks
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HvaSlagsTypeBillettMaDuKjopeType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HvorSkalDuKjoreMedEgenBilType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HvorforIkkeBilType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.JaNeiType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.KanDuReiseMedOffentligTransportType
import no.nav.tilleggsstonader.kontrakter.søknad.felles.TypePengestøtte
import no.nav.tilleggsstonader.kontrakter.søknad.felles.ÅrsakOppholdUtenforNorge
import no.nav.tilleggsstonader.sak.opplysninger.kodeverk.KodeverkService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Adresse
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.AktivitetAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.ArbeidOgOpphold
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.OppholdUtenforNorge
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Personopplysninger
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadDagligReise
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.ValgtAktivitet
import no.nav.tilleggsstonader.sak.vedlegg.BrevkodeVedlegg
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.ArbeidOgOpphold as ArbeidOgOppholdKontrakt
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.OppholdUtenforNorge as OppholdUtenforNorgeKontrakt
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Reise as ReiseKontrakt
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.SkjemaDagligReise as SkjemaDagligReiseKontrakt
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.TypeUtdanning as TypeUtdanningKontrakter

@Service
class SøknadskjemaDagligReiseMapper(
    private val kodeverkService: KodeverkService,
) {
    fun map(
        mottattTidspunkt: LocalDateTime,
        språk: Språkkode,
        journalpost: Journalpost,
        skjema: SøknadsskjemaDagligReiseFyllUtSendInn,
    ): SøknadDagligReise =
        SøknadDagligReise(
            journalpostId = journalpost.journalpostId,
            mottattTidspunkt = mottattTidspunkt,
            språk = språk,
            data = mapSkjema(skjema, mapDokumentasjon(journalpost.dokumenter)),
        )

    private fun mapDokumentasjon(dokumenter: List<DokumentInfo>?): List<DokumentasjonDagligReise> {
        if (dokumenter == null) return emptyList()
        return dokumenter
            .filter { it.brevkode != DokumentBrevkode.DAGLIG_REISE.verdi }
            .filter { it.brevkode != BrevkodeVedlegg.INNSENDINGSKVITTERING.kode }
            .mapNotNull { dokument ->
                dokument.tittel?.let {
                    DokumentasjonDagligReise(tittel = it, dokumentInfoId = dokument.dokumentInfoId)
                }
            }
    }

    fun mapSkjema(
        dagligReise: SøknadsskjemaDagligReiseFyllUtSendInn,
        dokumentasjon: List<DokumentasjonDagligReise>,
    ): SkjemaDagligReise {
        val skjemaDagligReise = dagligReise.data.data
        return mapSkjema(skjemaDagligReise, dokumentasjon)
    }

    private fun mapSkjema(
        skjemaDagligReise: SkjemaDagligReiseKontrakt,
        dokumentasjon: List<DokumentasjonDagligReise>,
    ): SkjemaDagligReise =
        SkjemaDagligReise(
            personopplysninger = mapPersonopplysninger(skjemaDagligReise.dineOpplysninger),
            hovedytelse = mapHovedytelse(skjemaDagligReise),
            aktivitet = mapAktivitet(skjemaDagligReise.aktiviteter),
            reiser = mapReiser(skjemaDagligReise.reise),
            dokumentasjon = dokumentasjon,
        )

    private fun mapPersonopplysninger(opplysninger: DineOpplysninger): Personopplysninger =
        Personopplysninger(
            adresse =
                opplysninger.adresse?.let {
                    Adresse(
                        gyldigFraOgMed = it.gyldigFraOgMed,
                        adresse = it.adresse,
                        postnummer = it.postnummer,
                        poststed = it.bySted,
                        landkode = it.land?.value?.let { land -> kodeverkService.hentLandkodeIso2(land) },
                    )
                },
            fødselsdatoPersonUtenFødselsnummer = opplysninger.fodselsdato2,
        )

    private fun mapHovedytelse(skjemaDagligReise: SkjemaDagligReiseKontrakt) =
        HovedytelseAvsnitt(
            hovedytelse = mapHovedytelse(skjemaDagligReise.hovedytelse),
            harNedsattArbeidsevne = null, // Finnes ikke i daglig reise søknad
            arbeidOgOpphold = mapArbeidOgOpphold(skjemaDagligReise.arbeidOgOpphold),
        )

    /**
     * FyllUt/SendInn bruker Alpha-2 mens vi ellers bruker Alpha-3
     */
    private fun mapArbeidOgOpphold(arbeidOgOpphold: ArbeidOgOppholdKontrakt?): ArbeidOgOpphold? =
        arbeidOgOpphold?.let {
            ArbeidOgOpphold(
                jobberIAnnetLand = mapJaNei(it.jobberIAnnetLand),
                jobbAnnetLand = it.jobbAnnetLand?.value?.let { kodeverkService.hentLandkodeIso2(it) },
                harPengestøtteAnnetLand = mapPengestøtteAnnetLand(it.harPengestotteAnnetLand),
                pengestøtteAnnetLand = it.pengestotteAnnetLand?.value?.let { kodeverkService.hentLandkodeIso2(it) },
                harOppholdUtenforNorgeSiste12mnd = mapJaNei(it.harOppholdUtenforNorgeSiste12mnd),
                oppholdUtenforNorgeSiste12mnd = mapOpphold(it.oppholdUtenforNorgeSiste12mnd),
                harOppholdUtenforNorgeNeste12mnd = it.harOppholdUtenforNorgeNeste12mnd?.let { mapJaNei(it) },
                oppholdUtenforNorgeNeste12mnd = mapOpphold(it.oppholdUtenforNorgeNeste12mnd),
            )
        }

    private fun mapPengestøtteAnnetLand(pengestøtteAnnetLand: Map<HarPengestotteAnnetLandType, Boolean>): List<TypePengestøtte> =
        pengestøtteAnnetLand
            .filter { it.value }
            .map {
                when (it.key) {
                    HarPengestotteAnnetLandType.sykepenger -> TypePengestøtte.SYKEPENGER
                    HarPengestotteAnnetLandType.pensjon -> TypePengestøtte.PENSJON
                    HarPengestotteAnnetLandType.annenPengestotte -> TypePengestøtte.ANNEN_PENGESTØTTE
                    HarPengestotteAnnetLandType.mottarIkkePengestotte -> TypePengestøtte.MOTTAR_IKKE
                }
            }

    private fun mapOpphold(opphold: OppholdUtenforNorgeKontrakt?): List<OppholdUtenforNorge> =
        opphold?.let {
            listOf(
                OppholdUtenforNorge(
                    land = it.land.value,
                    årsak = mapÅrsakOppholdUtenforNorge(it.arsakOppholdUtenforNorge),
                    fom = it.periode.fom,
                    tom = it.periode.tom,
                ),
            )
        } ?: emptyList()

    private fun mapAktivitet(aktiviteter: Aktiviteter): AktivitetDagligReiseAvsnitt {
        val aktivitetListe =
            aktiviteter.aktiviteterOgMaalgruppe?.aktivitet?.let {
                // Fyll ut setter aktivitetId til "ingenAktivitet" og vi har ellers mapping til ANNET som brukes i vår søknad
                val id = if (it.aktivitetId == "ingenAktivitet") "ANNET" else it.aktivitetId
                listOf(ValgtAktivitet(id = id, label = it.text))
            }

        val aktivitetAvsnitt =
            AktivitetAvsnitt(
                aktiviteter = aktivitetListe,
                annenAktivitet = mapAnnenAktivitet(aktiviteter.arbeidsrettetAktivitet),
                lønnetAktivitet = null,
            )

        val dekkesUtgiftenAvAndre =
            DekkesUtgiftenAvAndre(
                typeUtdanning =
                    mapTypeUtdanning(
                        aktiviteter.faktiskeUtgifter.garDuPaVideregaendeEllerGrunnskole,
                    ),
                lærling = aktiviteter.faktiskeUtgifter.erDuLaerling?.let(::mapJaNei),
                arbeidsgiverDekkerUtgift = aktiviteter.faktiskeUtgifter.arbeidsgiverDekkerUtgift?.let(::mapJaNei),
                mottarIkkeSkoleskyss = aktiviteter.faktiskeUtgifter.bekreftelsemottarIkkeSkoleskyss,
                lønnetAktivitet = aktiviteter.faktiskeUtgifter.lonnGjennomTiltak?.let(::mapJaNei),
            )

        return AktivitetDagligReiseAvsnitt(
            aktivitet = aktivitetAvsnitt,
            dekkesUtgiftenAvAndre = dekkesUtgiftenAvAndre,
        )
    }

    private fun mapTypeUtdanning(vidregåendeEllerGrunnskole: TypeUtdanningKontrakter): TypeUtdanning =
        when (vidregåendeEllerGrunnskole) {
            TypeUtdanningKontrakter.videregaendeSkole -> TypeUtdanning.VIDEREGÅENDE
            TypeUtdanningKontrakter.opplaeringForVoksne -> TypeUtdanning.OPPLÆRING_FOR_VOKSNE
            TypeUtdanningKontrakter.annetTiltak -> TypeUtdanning.ANNET_TILTAK
        }

    private fun mapAnnenAktivitet(verdi: ArbeidsrettetAktivitetType?): AnnenAktivitetType? =
        when (verdi) {
            null -> null
            ArbeidsrettetAktivitetType.tiltakArbeidsrettetUtredning -> AnnenAktivitetType.TILTAK
            ArbeidsrettetAktivitetType.utdanningGodkjentAvNav -> AnnenAktivitetType.UTDANNING
            ArbeidsrettetAktivitetType.harIngenArbeidsrettetAktivitet -> AnnenAktivitetType.INGEN_AKTIVITET
            ArbeidsrettetAktivitetType.jegErArbeidssoker -> AnnenAktivitetType.ARBEIDSSØKER
        }

    private fun mapReiser(reiser: List<ReiseKontrakt>): List<Reise> =
        reiser.map { reise ->
            val kanReiseMedOffentligTransport = mapKanReiseMedOffentligTransport(reise.kanDuReiseMedOffentligTransport)

            Reise(
                adresse =
                    ReiseAdresse(
                        gateadresse = reise.gateadresse,
                        postnummer = reise.postnr,
                        poststed = reise.poststed,
                    ),
                periode = Reiseperiode(fom = reise.fom, tom = reise.tom),
                dagerPerUke = reise.hvorMangeDagerIUkenSkalDuMoteOppPaAktivitetstedet.value,
                harMerEnn6KmReisevei = mapJaNei(reise.harDu6KmReisevei),
                lengdeReisevei = reise.hvorLangErReiseveienDin,
                harBehovForTransportUavhengigAvReisensLengde =
                    reise.harDuAvMedisinskeArsakerBehovForTransportUavhengigAvReisensLengde?.let(
                        ::mapJaNei,
                    ),
                kanReiseMedOffentligTransport = kanReiseMedOffentligTransport,
                offentligTransport =
                    if (kanReiseMedOffentligTransport.kanReiseMedOffentligTransport()) {
                        mapOffentligTransport(
                            reise,
                        )
                    } else {
                        null
                    },
                privatTransport = mapPrivatTransport(reise),
            )
        }

    private fun mapKanReiseMedOffentligTransport(
        kanDuReiseMedOffentligTransport: KanDuReiseMedOffentligTransportType,
    ): KanDuReiseMedOffentligTransport =
        when (kanDuReiseMedOffentligTransport) {
            KanDuReiseMedOffentligTransportType.ja -> KanDuReiseMedOffentligTransport.JA
            KanDuReiseMedOffentligTransportType.nei -> KanDuReiseMedOffentligTransport.NEI
            KanDuReiseMedOffentligTransportType.kombinertTogBil -> KanDuReiseMedOffentligTransport.KOMBINERT_BIL_OFFENTLIG_TRANSPORT
        }

    private fun mapOffentligTransport(reise: ReiseKontrakt): OffentligTransport {
        val billettTyper =
            reise.hvaSlagsTypeBillettMaDuKjope
                ?.filter { it.value }
                ?.map {
                    when (it.key) {
                        HvaSlagsTypeBillettMaDuKjopeType.enkeltbillett -> BillettType.ENKELTBILLETT
                        HvaSlagsTypeBillettMaDuKjopeType.ukeskort -> BillettType.SYVDAGERSBILLETT
                        HvaSlagsTypeBillettMaDuKjopeType.manedskort -> BillettType.TRETTIDAGERSBILLETT
                    }
                } ?: emptyList()

        return OffentligTransport(
            billettTyperValgt = billettTyper,
            enkeltbillettPris = reise.enkeltbilett,
            syvdagersbillettPris = reise.syvdagersbilett,
            månedskortPris = reise.manedskort,
        )
    }

    private fun mapPrivatTransport(reise: ReiseKontrakt): PrivatTransport? {
        val årsakIkkeOffentligTransport =
            reise.hvaErViktigsteGrunnerTilAtDuIkkeKanBrukeOffentligTransport
                ?.filter { it.value }
                ?.map {
                    when (it.key) {
                        helsemessigeArsaker -> ÅrsakIkkeOffentligTransport.HELSEMESSIGE_ÅRSAKER
                        darligTransporttilbud -> ÅrsakIkkeOffentligTransport.DÅRLIG_TRANSPORTTILBUD
                        leveringHentingIBarnehageEllerSkolefritidsordningSfoAks,
                        -> ÅrsakIkkeOffentligTransport.LEVERING_HENTING_BARNEHAGE_SKOLE

                        annet -> ÅrsakIkkeOffentligTransport.ANNET
                    }
                } ?: emptyList()

        return if (reise.kanDuReiseMedOffentligTransport === KanDuReiseMedOffentligTransportType.kombinertTogBil ||
            reise.kanDuReiseMedOffentligTransport == KanDuReiseMedOffentligTransportType.nei
        ) {
            PrivatTransport(
                årsakIkkeOffentligTransport = årsakIkkeOffentligTransport,
                kanKjøreMedEgenBil =
                    mapJaNei(
                        reise.kanKjoreMedEgenBil
                            ?: error("'Kan kjøre egen bil' er påkrevd om bruker skal bruke privat transport"),
                    ),
                utgifterBil = mapUtgifterBil(reise),
                taxi = mapTaxi(reise),
            )
        } else {
            null
        }
    }

    private fun mapUtgifterBil(reise: ReiseKontrakt): UtgifterBil? {
        val skalKjøreBil =
            reise.kanKjoreMedEgenBil == JaNeiType.ja ||
                reise.kanDuReiseMedOffentligTransport == KanDuReiseMedOffentligTransportType.kombinertTogBil
        return if (skalKjøreBil) {
            UtgifterBil(
                destinasjonEgenBil = mapDestinasjonEgenBil(reise.hvorSkalDuKjoreMedEgenBil),
                parkering = mapJaNei(reise.parkering ?: error("'Parkering' er påkrevd når bruker skal kjøre egen bil")),
                mottarGrunnstønad = reise.mottarDuGrunnstonadFraNav?.let(::mapJaNei),
                reisedistanseEgenBil =
                    reise.hvorLangErReiseveienDinMedBil
                        ?: error("'Reisedistanse med egen bil' er påkrevd når bruker skal kjøre egen bil"),
                bompenger = reise.bompenger,
                ferge = reise.ferge,
                piggdekkavgift = reise.piggdekkavgift,
            )
        } else {
            null
        }
    }

    private fun mapTaxi(reise: ReiseKontrakt): Taxi? {
        val skalTaTaxi =
            reise.kanDuReiseMedOffentligTransport == KanDuReiseMedOffentligTransportType.nei && reise.kanKjoreMedEgenBil == JaNeiType.nei
        return if (skalTaTaxi) {
            Taxi(
                årsakIkkeKjøreBil =
                    mapÅrsakerIkkeEgenBil(reise.hvorforIkkeBil)
                        ?: error("Årasker til hvorfor bruker ikke kan kjøre egen bil er påkrevd når bruker skal ta taxi"),
                ønskerSøkeOmTaxi =
                    mapJaNei(
                        reise.reiseMedTaxi
                            ?: error("Ønsker å søke om taxi er påkrevd når bruker skal ta taxi"),
                    ),
                ttkort = reise.ttKort?.let(::mapJaNei),
            )
        } else {
            null
        }
    }

    private fun mapDestinasjonEgenBil(hvorSkalDuKjoreMedEgenBil: Map<HvorSkalDuKjoreMedEgenBilType, Boolean>?): List<DestinasjonEgenBil>? =
        hvorSkalDuKjoreMedEgenBil?.filter { it.value }?.map {
            when (it.key) {
                HvorSkalDuKjoreMedEgenBilType.togstasjon -> DestinasjonEgenBil.TOGSTASJON
                HvorSkalDuKjoreMedEgenBilType.busstopp -> DestinasjonEgenBil.BUSSSTOPP
                HvorSkalDuKjoreMedEgenBilType.fergeBatkai -> DestinasjonEgenBil.FERGE_BAT_KAI
            }
        }

    private fun mapÅrsakerIkkeEgenBil(hvorforIkkeBil: Map<HvorforIkkeBilType, Boolean>?): List<ÅrsakIkkeKjøreBil>? =
        hvorforIkkeBil?.filter { svar -> svar.value }?.map { svar ->
            when (svar.key) {
                HvorforIkkeBilType.helsemessigeArsaker -> ÅrsakIkkeKjøreBil.HELSEMESSIGE_ÅRSAKER
                HvorforIkkeBilType.harIkkeBilEllerForerkort -> ÅrsakIkkeKjøreBil.HAR_IKKE_BIL_FØRERKORT
                HvorforIkkeBilType.annet -> ÅrsakIkkeKjøreBil.ANNET
            }
        }

    private fun mapJaNei(verdi: JaNeiType): JaNei =
        when (verdi) {
            JaNeiType.ja -> JaNei.JA
            JaNeiType.nei -> JaNei.NEI
        }

    private fun mapHovedytelse(hovedytelse: Map<HovedytelseType, Boolean>): List<Hovedytelse> =
        hovedytelse
            .filter { it.value }
            .map {
                when (it.key) {
                    HovedytelseType.arbeidsavklaringspenger -> Hovedytelse.AAP
                    HovedytelseType.overgangsstonad -> Hovedytelse.OVERGANGSSTØNAD
                    HovedytelseType.gjenlevendepensjon -> Hovedytelse.GJENLEVENDEPENSJON
                    HovedytelseType.uforetrygd -> Hovedytelse.UFØRETRYGD
                    HovedytelseType.tiltakspenger -> Hovedytelse.TILTAKSPENGER
                    HovedytelseType.dagpenger -> Hovedytelse.DAGPENGER
                    HovedytelseType.sykepenger -> Hovedytelse.SYKEPENGER
                    HovedytelseType.kvalifiseringsstonad -> Hovedytelse.KVALIFISERINGSSTØNAD
                    HovedytelseType.mottarIngenPengestotte -> Hovedytelse.INGEN_PENGESTØTTE
                    HovedytelseType.ingenAvAlternativenePasserForMeg -> Hovedytelse.INGEN_PASSENDE_ALTERNATIVER
                }
            }

    private fun mapÅrsakOppholdUtenforNorge(
        arsakOppholdUtenforNorge: Map<ArsakOppholdUtenforNorgeType, Boolean>,
    ): List<ÅrsakOppholdUtenforNorge> =
        arsakOppholdUtenforNorge
            .filter { it.value }
            .map {
                when (it.key) {
                    ArsakOppholdUtenforNorgeType.jobbet -> ÅrsakOppholdUtenforNorge.JOBB
                    ArsakOppholdUtenforNorgeType.studerte -> ÅrsakOppholdUtenforNorge.STUDIER
                    ArsakOppholdUtenforNorgeType.fikkMedisinskBehandling -> ÅrsakOppholdUtenforNorge.MEDISINSK_BEHANDLING
                    ArsakOppholdUtenforNorgeType.varPaFerie -> ÅrsakOppholdUtenforNorge.FERIE
                    ArsakOppholdUtenforNorgeType.besokteFamilie -> ÅrsakOppholdUtenforNorge.FAMILIE_BESØK
                    ArsakOppholdUtenforNorgeType.annet -> ÅrsakOppholdUtenforNorge.ANNET
                }
            }
}
