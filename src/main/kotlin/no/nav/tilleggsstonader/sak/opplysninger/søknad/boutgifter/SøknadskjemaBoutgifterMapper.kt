package no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaBoutgifterFyllUtSendInn
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.AnnenAktivitetType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.Aktiviteter
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.ArbeidsrettetAktivitetType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.ArsakOppholdUtenforNorgeType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.BoligEllerOvernatting
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.DelerBoutgifterType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.DineOpplysninger
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.HarPengestotteAnnetLandType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.HarUtgifterTilBoligToStederType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.HovedytelseType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.JaNeiType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.Samling
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.TypeUtgifterType
import no.nav.tilleggsstonader.kontrakter.søknad.felles.TypePengestøtte
import no.nav.tilleggsstonader.kontrakter.søknad.felles.ÅrsakOppholdUtenforNorge
import no.nav.tilleggsstonader.sak.opplysninger.kodeverk.KodeverkService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.AktivitetAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.ArbeidOgOpphold
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.OppholdUtenforNorge
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBoutgifter
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.ValgtAktivitet
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.ArbeidOgOpphold as ArbeidOgOppholdKontrakt
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.FasteUtgifter as FasteUtgifterKontrakt
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.OppholdUtenforNorge as OppholdUtenforNorgeKontrakt
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.SkjemaBoutgifter as SkjemaBoutgifterKontrakt
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.UtgifterFlereSteder as UtgifterFlereStederKontrakt
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.UtgifterNyBolig as UtgifterNyBoligKontrakt

@Service
class SøknadskjemaBoutgifterMapper(
    private val kodeverkService: KodeverkService,
) {
    fun map(
        mottattTidspunkt: LocalDateTime,
        språk: Språkkode,
        journalpost: Journalpost,
        skjema: SøknadsskjemaBoutgifterFyllUtSendInn,
    ): SøknadBoutgifter =
        SøknadBoutgifter(
            journalpostId = journalpost.journalpostId,
            mottattTidspunkt = mottattTidspunkt,
            språk = språk,
            data = mapSkjema(skjema, mapDokumentasjon(journalpost.dokumenter)),
        )

    private fun mapDokumentasjon(dokumenter: List<DokumentInfo>?): List<DokumentasjonBoutgifter> {
        if (dokumenter == null) return emptyList()
        return dokumenter
            .filter { it.brevkode != DokumentBrevkode.BOUTGIFTER.verdi }
            .filter { it.brevkode != "L7" && it.tittel != "Innsendingskvittering" } // Innsendingskvittering
            .mapNotNull { dokument ->
                dokument.tittel?.let {
                    DokumentasjonBoutgifter(tittel = it, dokumentInfoId = dokument.dokumentInfoId)
                }
            }
    }

    fun mapSkjema(
        boutgifter: SøknadsskjemaBoutgifterFyllUtSendInn,
        dokumentasjon: List<DokumentasjonBoutgifter>,
    ): SkjemaBoutgifter {
        val skjemaBoutgifter = boutgifter.data.data
        return mapSkjema(skjemaBoutgifter, dokumentasjon)
    }

    private fun mapSkjema(
        skjemaBoutgifter: SkjemaBoutgifterKontrakt,
        dokumentasjon: List<DokumentasjonBoutgifter>,
    ): SkjemaBoutgifter =
        SkjemaBoutgifter(
            personopplysninger = mapPersonopplysninger(skjemaBoutgifter.dineOpplysninger),
            hovedytelse = mapHovedytelse(skjemaBoutgifter),
            aktivitet = mapAktivitet(skjemaBoutgifter.aktiviteter),
            boutgifter = mapBoutgifter(skjemaBoutgifter.boligEllerOvernatting),
            harNedsattArbeidsevne = skjemaBoutgifter.harNedsattArbeidsevne?.let { mapJaNei(it) },
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
                        landkode = kodeverkService.hentLandkodeIso2(it.land.value),
                    )
                },
        )

    private fun mapHovedytelse(skjemaBoutgifter: SkjemaBoutgifterKontrakt) =
        HovedytelseAvsnitt(
            hovedytelse = mapHovedytelse(skjemaBoutgifter.hovedytelse),
            harNedsattArbeidsevne = skjemaBoutgifter.harNedsattArbeidsevne?.let { mapJaNei(it) },
            arbeidOgOpphold = mapArbeidOgOpphold(skjemaBoutgifter.arbeidOgOpphold),
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

    private fun mapAktivitet(aktiviteter: Aktiviteter): AktivitetAvsnitt {
        val aktivitet = aktiviteter.aktiviteterOgMaalgruppe.aktivitet
        // Fyll ut setter aktivitetId til "ingenAktivitet" og vi har ellers mapping til ANNET som brukes i vår søknad
        val id = if (aktivitet.aktivitetId == "ingenAktivitet") "ANNET" else aktivitet.aktivitetId
        return AktivitetAvsnitt(
            aktiviteter = listOf(ValgtAktivitet(id = id, label = aktivitet.text)),
            annenAktivitet = mapAnnenAktivitet(aktiviteter.arbeidsrettetAktivitet),
            lønnetAktivitet = aktiviteter.mottarLonnGjennomTiltak?.let(::mapJaNei),
        )
    }

    private fun mapAnnenAktivitet(verdi: ArbeidsrettetAktivitetType?): AnnenAktivitetType? =
        when (verdi) {
            null -> null
            ArbeidsrettetAktivitetType.tiltakArbeidsrettetUtredning -> AnnenAktivitetType.TILTAK
            ArbeidsrettetAktivitetType.utdanningGodkjentAvNav -> AnnenAktivitetType.UTDANNING
            ArbeidsrettetAktivitetType.harIngenArbeidsrettetAktivitet -> AnnenAktivitetType.INGEN_AKTIVITET
        }

    private fun mapBoutgifter(boligEllerOvernatting: BoligEllerOvernatting): BoligEllerOvernattingAvsnitt =
        BoligEllerOvernattingAvsnitt(
            typeUtgifter = mapTypeUtgifter(boligEllerOvernatting.typeUtgifter),
            fasteUtgifter = mapFasteUtgifter(boligEllerOvernatting.fasteUtgifter),
            samling = mapSamling(boligEllerOvernatting.samling),
            harSærligStoreUtgifterPgaFunksjonsnedsettelse =
                mapJaNei(boligEllerOvernatting.harSaerligStoreUtgifterPaGrunnAvFunksjonsnedsettelse),
        )

    private fun mapTypeUtgifter(verdi: TypeUtgifterType): TypeUtgifter =
        when (verdi) {
            TypeUtgifterType.fastUtgift -> TypeUtgifter.FASTE
            TypeUtgifterType.midlertidigUtgift -> TypeUtgifter.SAMLING
        }

    private fun mapFasteUtgifter(fasteUtgifter: FasteUtgifterKontrakt?): FasteUtgifter? =
        fasteUtgifter?.let { utgifter ->
            FasteUtgifter(
                typeFasteUtgifter = mapTypeFasteUtgifter(utgifter.harUtgifterTilBoligToSteder),
                utgifterFlereSteder = mapUtgifterFlereSteder(utgifter.utgifterFlereSteder),
                utgifterNyBolig = mapUtgifterNyBolig(utgifter.utgifterNyBolig),
            )
        }

    private fun mapUtgifterNyBolig(utgifterNyBolig: UtgifterNyBoligKontrakt?): UtgifterNyBolig? =
        utgifterNyBolig?.let {
            UtgifterNyBolig(
                delerBoutgifter = mapJaNei(it.delerBoutgifter),
                andelUtgifterBolig = it.andelUtgifterBolig,
                harHoyereUtgifterPaNyttBosted = mapJaNei(it.harHoyereUtgifterPaNyttBosted),
                mottarBostotte = it.mottarBostotte?.let(::mapJaNei),
            )
        }

    private fun mapUtgifterFlereSteder(utgifterFlereSteder: UtgifterFlereStederKontrakt?): UtgifterFlereSteder? =
        utgifterFlereSteder?.let {
            UtgifterFlereSteder(
                delerBoutgifter = mapDelerBoutgifterFlereSteder(it.delerBoutgifter),
                andelUtgifterBoligHjemsted = it.andelUtgifterBoligHjemsted,
                andelUtgifterBoligAktivitetssted = it.andelUtgifterBoligAktivitetssted,
            )
        }

    private fun mapTypeFasteUtgifter(verdi: HarUtgifterTilBoligToStederType): TypeFasteUtgifter =
        when (verdi) {
            HarUtgifterTilBoligToStederType.ekstraBolig -> TypeFasteUtgifter.EKSTRA_BOLIG
            HarUtgifterTilBoligToStederType.nyBolig -> TypeFasteUtgifter.NY_BOLIG
        }

    private fun mapSamling(samling: Samling?): UtgifterIForbindelseMedSamling? =
        samling?.let { utgifter ->
            UtgifterIForbindelseMedSamling(
                periodeForSamling =
                    utgifter.periodeForSamling.map {
                        PeriodeForSamling(
                            fom = it.fom,
                            tom = it.tom,
                            trengteEkstraOvernatting = mapJaNei(it.trengteEkstraOvernatting),
                            utgifterTilOvernatting = it.utgifterTilOvernatting,
                        )
                    },
            )
        }

    private fun mapJaNei(verdi: JaNeiType): JaNei =
        when (verdi) {
            JaNeiType.ja -> JaNei.JA
            JaNeiType.nei -> JaNei.NEI
        }

    private fun mapDelerBoutgifterFlereSteder(typer: Map<DelerBoutgifterType, Boolean>): List<DelerUtgifterFlereStederType> =
        typer
            .filter { it.value }
            .map {
                when (it.key) {
                    DelerBoutgifterType.hjemsted -> DelerUtgifterFlereStederType.HJEMSTED
                    DelerBoutgifterType.aktivitetssted -> DelerUtgifterFlereStederType.AKTIVITETSSTED
                    DelerBoutgifterType.nei -> DelerUtgifterFlereStederType.NEI
                }
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
