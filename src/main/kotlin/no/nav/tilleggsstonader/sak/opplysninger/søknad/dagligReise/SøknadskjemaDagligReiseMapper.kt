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
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.DagligReiseFyllUtSendInnData
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.DineOpplysninger
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.FaktiskeUtgifter
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.GarDuPaVideregaendeEllerGrunnskoleType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HarPengestotteAnnetLandType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HovedytelseType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HvaErViktigsteGrunnerTilAtDuIkkeKanBrukeOffentligTransportType.darligTransporttilbud
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HvaErViktigsteGrunnerTilAtDuIkkeKanBrukeOffentligTransportType.helsemessigeArsaker
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HvaErViktigsteGrunnerTilAtDuIkkeKanBrukeOffentligTransportType.leveringHentingIBarnehageEllerSkolefritidsordningSfoAks
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HvaSlagsTypeBillettMaDuKjopeType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HvorforIkkeBilType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.JaNeiType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.MetadataDagligReise
import no.nav.tilleggsstonader.kontrakter.søknad.felles.TypePengestøtte
import no.nav.tilleggsstonader.kontrakter.søknad.felles.ÅrsakOppholdUtenforNorge
import no.nav.tilleggsstonader.sak.opplysninger.kodeverk.KodeverkService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Adresse
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.ArbeidOgOpphold
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.OppholdUtenforNorge
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Personopplysninger
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadDagligReise
import no.nav.tilleggsstonader.sak.vedlegg.BrevkodeVedlegg
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.ArbeidOgOpphold as ArbeidOgOppholdKontrakt
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.OppholdUtenforNorge as OppholdUtenforNorgeKontrakt
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Reise as ReiseKontrakt
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.SkjemaDagligReise as SkjemaDagligReiseKontrakt

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
        val søknadsdataDagligReise = dagligReise.data
        return mapSkjema(søknadsdataDagligReise, dokumentasjon)
    }

    private fun mapSkjema(
        søknadsdataDagligReise: DagligReiseFyllUtSendInnData,
        dokumentasjon: List<DokumentasjonDagligReise>,
    ): SkjemaDagligReise =
        SkjemaDagligReise(
            personopplysninger = mapPersonopplysninger(opplysninger = søknadsdataDagligReise.data.dineOpplysninger),
            hovedytelse = mapHovedytelse(skjemaDagligReise = søknadsdataDagligReise.data),
            aktivitet = mapAktivitet(aktiviteter = søknadsdataDagligReise.data.aktiviteter, metadata = søknadsdataDagligReise.metadata),
            reiser = mapReiser(reiser = søknadsdataDagligReise.data.reise, dineOpplysninger = søknadsdataDagligReise.data.dineOpplysninger),
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

    private fun mapAktivitet(
        aktiviteter: Aktiviteter,
        metadata: MetadataDagligReise,
    ): AktivitetDagligReiseAvsnitt {
        val valgteAktiviteter = aktiviteter.aktiviteterOgMaalgruppe?.filterValues { it }?.keys

        val aktiviteterDagligReise =
            valgteAktiviteter?.let {
                metadata.dataFetcher.aktiviteter.aktiviteterOgMaalgruppe.data
                    ?.filter { valgteAktiviteter.contains(it.value) }
                    ?.map {
                        AktivitetDagligReise(
                            id = it.value,
                            label = it.label,
                            type = it.type,
                        )
                    }
            }

        return AktivitetDagligReiseAvsnitt(
            aktiviteter = aktiviteterDagligReise,
            annenAktivitet = mapAnnenAktivitet(aktiviteter.arbeidsrettetAktivitet),
            dekkesUtgiftenAvAndre = mapDekkesUtgiftenAvAndre(aktiviteter.faktiskeUtgifter),
        )
    }

    private fun mapDekkesUtgiftenAvAndre(faktiskeUtgifter: FaktiskeUtgifter?): DekkesUtgiftenAvAndre? {
        if (faktiskeUtgifter == null) {
            return null
        }

        return DekkesUtgiftenAvAndre(
            typeUtdanning =
                mapTypeUtdanning(
                    faktiskeUtgifter.garDuPaVideregaendeEllerGrunnskole,
                ),
            lærling = faktiskeUtgifter.erDuLaerling?.let(::mapJaNei),
            arbeidsgiverDekkerUtgift = faktiskeUtgifter.arbeidsgiverDekkerUtgift?.let(::mapJaNei),
            erUnder25år = faktiskeUtgifter.under25?.let(::mapJaNei),
            betalerForReisenTilSkolenSelv = faktiskeUtgifter.betalerForReisenTilSkolenSelv?.let(::mapJaNei),
            lønnetAktivitet = faktiskeUtgifter.lonnGjennomTiltak?.let(::mapJaNei),
        )
    }

    private fun mapTypeUtdanning(videregåendeEllerGrunnskole: GarDuPaVideregaendeEllerGrunnskoleType?): TypeUtdanning? =
        when (videregåendeEllerGrunnskole) {
            null -> null
            GarDuPaVideregaendeEllerGrunnskoleType.videregaendeSkole -> TypeUtdanning.VIDEREGÅENDE
            GarDuPaVideregaendeEllerGrunnskoleType.opplaeringForVoksne -> TypeUtdanning.OPPLÆRING_FOR_VOKSNE
            GarDuPaVideregaendeEllerGrunnskoleType.annetTiltak -> TypeUtdanning.ANNET_TILTAK
        }

    private fun mapAnnenAktivitet(verdi: ArbeidsrettetAktivitetType?): AnnenAktivitetType? =
        when (verdi) {
            null -> null
            ArbeidsrettetAktivitetType.tiltakArbeidsrettetUtredning -> AnnenAktivitetType.TILTAK
            ArbeidsrettetAktivitetType.utdanningGodkjentAvNav -> AnnenAktivitetType.UTDANNING
            ArbeidsrettetAktivitetType.harIngenArbeidsrettetAktivitet -> AnnenAktivitetType.INGEN_AKTIVITET
            ArbeidsrettetAktivitetType.jegErArbeidssoker -> AnnenAktivitetType.ARBEIDSSØKER
        }

    private fun mapReiser(
        reiser: List<ReiseKontrakt>,
        dineOpplysninger: DineOpplysninger,
    ): List<Reise> {
        val skalReiseFraFolkeregistrertAdresse = mapJaNei(dineOpplysninger.reiseFraFolkeregistrertAdr)
        val adresseDetSkalReisesFra =
            if (skalReiseFraFolkeregistrertAdresse === JaNei.JA) {
                dineOpplysninger.adresse?.let {
                    ReiseAdresse(
                        gateadresse = it.adresse,
                        postnummer = it.postnummer,
                        poststed = it.bySted,
                    )
                }
            } else {
                dineOpplysninger.adresseJegSkalReiseFra?.let {
                    ReiseAdresse(
                        gateadresse = it.gateadresse,
                        postnummer = it.postnr,
                        poststed = it.poststed,
                    )
                }
            }

        return reiser.map { reise ->
            val kanReiseMedOffentligTransport = mapJaNei(reise.kanDuReiseMedOffentligTransport)

            Reise(
                skalReiseFraFolkeregistrertAdresse = skalReiseFraFolkeregistrertAdresse,
                adresseDetSkalReisesFra = adresseDetSkalReisesFra,
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
                offentligTransport = mapOffentligTransport(reise),
                privatTransport = mapPrivatTransport(reise),
            )
        }
    }

    private fun mapOffentligTransport(reise: ReiseKontrakt): OffentligTransport? {
        if (reise.kanDuReiseMedOffentligTransport === JaNeiType.nei) {
            return null
        }
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
            enkeltbillettPris = reise.enkeltbillett,
            syvdagersbillettPris = reise.syvdagersbillett,
            månedskortPris = reise.manedskort,
        )
    }

    private fun mapPrivatTransport(reise: ReiseKontrakt): PrivatTransport? {
        if (reise.kanDuReiseMedOffentligTransport === JaNeiType.ja) {
            return null
        }

        val årsakIkkeOffentligTransport =
            reise.hvaErViktigsteGrunnerTilAtDuIkkeKanBrukeOffentligTransport
                ?.filter { it.value }
                ?.map {
                    when (it.key) {
                        helsemessigeArsaker -> ÅrsakIkkeOffentligTransport.HELSEMESSIGE_ÅRSAKER
                        darligTransporttilbud -> ÅrsakIkkeOffentligTransport.DÅRLIG_TRANSPORTTILBUD
                        leveringHentingIBarnehageEllerSkolefritidsordningSfoAks,
                        -> ÅrsakIkkeOffentligTransport.LEVERING_HENTING_BARNEHAGE_SKOLE
                    }
                } ?: emptyList()

        return PrivatTransport(
            årsakIkkeOffentligTransport = årsakIkkeOffentligTransport,
            kanKjøreMedEgenBil = reise.kanKjoreMedEgenBil?.let(::mapJaNei),
            utgifterBil = mapUtgifterBil(reise),
            taxi = mapTaxi(reise),
        )
    }

    private fun mapUtgifterBil(reise: ReiseKontrakt): UtgifterBil? {
        if (reise.kanKjoreMedEgenBil == JaNeiType.nei) {
            return null
        }

        return UtgifterBil(
            parkering = mapJaNei(reise.parkering ?: error("'Parkering' er påkrevd når bruker skal kjøre egen bil")),
            mottarGrunnstønad = reise.mottarDuGrunnstonadFraNav?.let(::mapJaNei),
            bompenger = reise.bompenger,
            ferge = reise.ferge,
            piggdekkavgift = reise.piggdekkavgift,
        )
    }

    private fun mapTaxi(reise: ReiseKontrakt): Taxi? {
        val skalTaTaxi =
            reise.kanDuReiseMedOffentligTransport == JaNeiType.nei && reise.kanKjoreMedEgenBil == JaNeiType.nei
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
