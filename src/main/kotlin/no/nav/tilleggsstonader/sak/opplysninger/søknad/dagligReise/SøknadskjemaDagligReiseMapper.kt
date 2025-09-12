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
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Drosje
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HarPengestotteAnnetLandType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HovedytelseType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HvaErViktigsteGrunnerTilAtDuIkkeKanBrukeOffentligTransportType.annet
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HvaErViktigsteGrunnerTilAtDuIkkeKanBrukeOffentligTransportType.darligTransporttilbud
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HvaErViktigsteGrunnerTilAtDuIkkeKanBrukeOffentligTransportType.helsemessigeArsaker
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HvaErViktigsteGrunnerTilAtDuIkkeKanBrukeOffentligTransportType.leveringHentingIBarnehageEllerSkolefritidsordningSfoAks
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HvaErViktigsteGrunnerTilAtDuIkkeKanKjoreBilType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HvaSlagsTypeBillettMaDuKjopeType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.JaNeiType
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
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.UtgifterBil as UtgifterBilKontrakt

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
            annenAdresseDetSkalReisesFra = mapAnnenAdresseDetSkalReisesFra(skjemaDagligReise.dineOpplysninger),
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
        )

    private fun mapAnnenAdresseDetSkalReisesFra(opplysninger: DineOpplysninger): ReiseAdresse? =
        if (opplysninger.reiseFraAnnetEnnFolkeregistrertAdr == JaNeiType.ja) {
            opplysninger.adresseJegSkalReiseFra?.let {
                ReiseAdresse(
                    gateadresse = it.gateadresse,
                    postnummer = it.postnr,
                    poststed = it.poststed,
                )
            }
        } else {
            null
        }

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
        val aktivitet = aktiviteter.aktiviteterOgMaalgruppe.aktivitet
        // Fyll ut setter aktivitetId til "ingenAktivitet" og vi har ellers mapping til ANNET som brukes i vår søknad
        val id = if (aktivitet.aktivitetId == "ingenAktivitet") "ANNET" else aktivitet.aktivitetId

        val aktivitetAvsnitt =
            AktivitetAvsnitt(
                aktiviteter = listOf(ValgtAktivitet(id = id, label = aktivitet.text)),
                annenAktivitet = mapAnnenAktivitet(aktiviteter.arbeidsrettetAktivitet),
                lønnetAktivitet = aktiviteter.mottarLonnGjennomTiltak?.let(::mapJaNei),
            )

        return AktivitetDagligReiseAvsnitt(
            aktivitet = aktivitetAvsnitt,
            reiseTilAktivitetsstedHelePerioden = aktiviteter.reiseTilAktivitetsstedHelePerioden?.let(::mapJaNei),
            reiseperiode =
                aktiviteter.reiseperiode?.let {
                    Reiseperiode(fom = it.fom, tom = it.tom)
                },
        )
    }

    private fun mapAnnenAktivitet(verdi: ArbeidsrettetAktivitetType?): AnnenAktivitetType? =
        when (verdi) {
            null -> null
            ArbeidsrettetAktivitetType.tiltakArbeidsrettetUtredning -> AnnenAktivitetType.TILTAK
            ArbeidsrettetAktivitetType.utdanningGodkjentAvNav -> AnnenAktivitetType.UTDANNING
            ArbeidsrettetAktivitetType.harIngenArbeidsrettetAktivitet -> AnnenAktivitetType.INGEN_AKTIVITET
        }

    private fun mapReiser(reiser: List<ReiseKontrakt>): List<Reise> =
        reiser.map { reise ->
            val kanReiseMedOffentligTransport = mapJaNei(reise.kanDuReiseMedOffentligTransport)

            Reise(
                reiseAdresse =
                    ReiseAdresse(
                        gateadresse = reise.reiseAdresse.gateadresse,
                        postnummer = reise.reiseAdresse.postnr,
                        poststed = reise.reiseAdresse.poststed,
                    ),
                dagerPerUke =
                    ValgtAktivitetDagligReise(
                        id = reise.hvorMangeDagerIUkenSkalDuMoteOppPaAktivitetstedet.value,
                        label = reise.hvorMangeDagerIUkenSkalDuMoteOppPaAktivitetstedet.label,
                    ),
                harMerEnn6KmReisevei = mapJaNei(reise.harDu6KmReisevei),
                lengdeReisevei = reise.hvorLangErReiseveienDin,
                harBehovForTransportUavhengigAvReisensLengde =
                    reise.harDuAvMedisinskeArsakerBehovForTransportUavhengigAvReisensLengde?.let(
                        ::mapJaNei,
                    ),
                kanReiseMedOffentligTransport = kanReiseMedOffentligTransport,
                offentligTransport = if (kanReiseMedOffentligTransport == JaNei.JA) mapOffentligTransport(reise) else null,
                privatTransport = mapPrivatTransport(reise),
            )
        }

    private fun mapOffentligTransport(reise: ReiseKontrakt): OffentligTransport {
        val billettTyper =
            reise.hvaSlagsTypeBillettMaDuKjope
                ?.filter { it.value }
                ?.map {
                    when (it.key) {
                        HvaSlagsTypeBillettMaDuKjopeType.enkeltbillett -> BillettType.ENKELTBILLETT
                        HvaSlagsTypeBillettMaDuKjopeType.ukeskort -> BillettType.SYVDAGERSBILLETT
                        HvaSlagsTypeBillettMaDuKjopeType.manedskort -> BillettType.MÅNEDSKORT
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

        val bilUtgifter = mapBilUtgifter(reise.utgifterBil)
        val taxiUtgifter = mapTaxiUtgifter(reise.drosje)

        return if (årsakIkkeOffentligTransport.isNotEmpty() ||
            reise.kanDuKjoreMedEgenBil != null ||
            bilUtgifter != null ||
            taxiUtgifter != null
        ) {
            PrivatTransport(
                årsakIkkeOffentligTransport = årsakIkkeOffentligTransport,
                kanKjøreMedEgenBil = reise.kanDuKjoreMedEgenBil?.let(::mapJaNei),
                utgifterBil = bilUtgifter,
                utgifterTaxi = taxiUtgifter,
            )
        } else {
            null
        }
    }

    private fun mapBilUtgifter(utgifterBil: UtgifterBilKontrakt?): UtgifterBil? =
        utgifterBil?.let {
            UtgifterBil(
                merEnn6kmReisevei = it.harDu6KmReisevei.let(::mapJaNei),
                bompenger = it.bompenger,
                ferge = it.ferge,
                piggdekkavgift = it.piggdekkavgift,
            )
        }

    private fun mapTaxiUtgifter(drosje: Drosje?): UtgifterTaxi? =
        drosje?.let {
            val årsakIkkeKjøreBil =
                it.hvaErViktigsteGrunnerTilAtDuIkkeKanKjoreBil
                    .filter { svar -> svar.value }
                    .map { svar ->
                        when (svar.key) {
                            HvaErViktigsteGrunnerTilAtDuIkkeKanKjoreBilType.helsemessigeArsaker -> ÅrsakIkkeKjøreBil.HELSEMESSIGE_ÅRSAKER
                            HvaErViktigsteGrunnerTilAtDuIkkeKanKjoreBilType.darligTransporttilbud,
                            -> ÅrsakIkkeKjøreBil.DÅRLIG_TRANSPORTTILBUD
                            HvaErViktigsteGrunnerTilAtDuIkkeKanKjoreBilType.annet -> ÅrsakIkkeKjøreBil.ANNET
                        }
                    }

            UtgifterTaxi(
                årsakIkkeKjøreBil = årsakIkkeKjøreBil,
                ønskerSøkeOmTaxi = mapJaNei(it.onskerDuASokeOmFaDekketUtgifterTilReiseMedTaxi),
            )
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
