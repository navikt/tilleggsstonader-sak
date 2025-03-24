package no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaBoutgifterFyllUtSendInn
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.AnnenAktivitetType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.ArsakOppholdUtenforNorgeType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.BoligEllerOvernatting
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.HovedytelseType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.Samling
import no.nav.tilleggsstonader.kontrakter.søknad.felles.ÅrsakOppholdUtenforNorge
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.AktivitetAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.ArbeidOgOpphold
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.OppholdUtenforNorge
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBoutgifter
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.ValgtAktivitet
import java.time.LocalDateTime
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.ArbeidOgOpphold as ArbeidOgOppholdKontrakt
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.FasteUtgifter as FasteUtgifterKontrakt
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.OppholdUtenforNorge as OppholdUtenforNorgeKontrakt
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.SkjemaBoutgifter as SkjemaBoutgifterKontrakt

/**
 * TODO burde mappe iso2landkoder til 3 her
 */
object SøknadskjemaBoutgifterMapper {
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

    fun mapDokumentasjon(dokumenter: List<DokumentInfo>?): List<DokumentasjonBoutgifter> {
        if (dokumenter == null) return emptyList()
        return dokumenter
            .filter { it.brevkode != DokumentBrevkode.BOUTGIFTER.verdi }
            .filter { it.brevkode != "L7" || it.tittel == "Innsendingskvittering" } // Innsendingskvittering
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
            hovedytelse = mapHovedytelse(skjemaBoutgifter),
            aktivitet = mapAktivitet(skjemaBoutgifter),
            boutgifter = mapBoutgifter(skjemaBoutgifter.boligEllerOvernatting),
            dokumentasjon = dokumentasjon,
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
                jobbAnnetLand = it.jobbAnnetLand?.value,
                harPengestøtteAnnetLand = null, // = mapJaNei(it.harPengestotteAnnetLand), TODO endre til checkbox?
                pengestøtteAnnetLand = it.pengestotteAnnetLand?.label,
                harOppholdUtenforNorgeSiste12mnd = mapJaNei(it.harOppholdUtenforNorgeSiste12mnd),
                oppholdUtenforNorgeSiste12mnd = mapOpphold(it.oppholdUtenforNorgeSiste12mnd),
                harOppholdUtenforNorgeNeste12mnd = it.harOppholdUtenforNorgeNeste12mnd?.let { mapJaNei(it) },
                oppholdUtenforNorgeNeste12mnd = mapOpphold(it.oppholdUtenforNorgeNeste12mnd),
            )
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

    private fun mapAktivitet(skjemaBoutgifter: SkjemaBoutgifterKontrakt): AktivitetAvsnitt {
        val aktivitet = skjemaBoutgifter.aktiviteter.aktiviteterOgMaalgruppe.aktivitet
        // Fyll ut setter aktivitetId til "ingenAktivitet" og vi har ellers mapping til ANNET som brukes i vår søknad
        val id = if (aktivitet.aktivitetId == "ingenAktivitet") "ANNET" else aktivitet.aktivitetId
        return AktivitetAvsnitt(
            aktiviteter = listOf(ValgtAktivitet(id = id, label = aktivitet.text)),
            annenAktivitet = mapAnnenAktivitet(skjemaBoutgifter.aktiviteter.arbeidsrettetAktivitet),
            lønnetAktivitet = null,
        )
    }

    private fun mapAnnenAktivitet(verdi: String?): AnnenAktivitetType? =
        when (verdi) {
            null -> null
            "tiltakArbeidsrettetUtredning" -> AnnenAktivitetType.TILTAK
            "utdanningGodkjentAvNav" -> AnnenAktivitetType.UTDANNING
            "harIngenArbeidsrettetAktivitet" -> AnnenAktivitetType.INGEN_AKTIVITET
            else -> error("Har ikke mapping av annenAktivitet=$verdi")
        }

    private fun mapBoutgifter(boligEllerOvernatting: BoligEllerOvernatting): BoligEllerOvernattingAvsnitt =
        BoligEllerOvernattingAvsnitt(
            typeUtgifter = mapTypeUtgifter(boligEllerOvernatting.typeUtgifter),
            fasteUtgifter = mapFasteUtgifter(boligEllerOvernatting.fasteUtgifter),
            samling = mapSamling(boligEllerOvernatting.samling),
            harSærligStoreUtgifterPgaFunksjonsnedsettelse =
                mapJaNei(boligEllerOvernatting.harSaerligStoreUtgifterPaGrunnAvFunksjonsnedsettelse),
        )

    private fun mapTypeUtgifter(verdi: String): TypeUtgifter =
        when (verdi) {
            "fastUtgift" -> TypeUtgifter.FASTE
            "midlertidigUtgift" -> TypeUtgifter.SAMLING
            else -> error("Ukjent verdi $verdi")
        }

    private fun mapFasteUtgifter(fasteUtgifter: FasteUtgifterKontrakt?): FasteUtgifter? =
        fasteUtgifter?.let { utgifter ->
            FasteUtgifter(
                harUtgifterTilBoligToSteder = mapTypeFasteUtgifter(utgifter.harUtgifterTilBoligToSteder),
                harLeieinntekterSomDekkerUtgifteneTilBoligenPaHjemstedet =
                    utgifter.harLeieinntekterSomDekkerUtgifteneTilBoligenPaHjemstedet?.let { mapJaNei(it) },
                harHoyereUtgifterPaNyttBosted = utgifter.harHoyereUtgifterPaNyttBosted?.let { mapJaNei(it) },
                mottarBostotte = utgifter.mottarBostotte?.let { mapJaNei(it) },
            )
        }

    private fun mapTypeFasteUtgifter(verdi: String): TypeFasteUtgifter =
        when (verdi) {
            "ekstraBolig" -> TypeFasteUtgifter.EKSTRA_BOLIG
            "nyBolig" -> TypeFasteUtgifter.NY_BOLIG
            else -> error("Ukjent verdi $verdi")
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

    private fun mapJaNei(verdi: String): JaNei =
        when (verdi) {
            "ja" -> JaNei.JA
            "nei" -> JaNei.NEI
            else -> error("Ukjent verdi $verdi")
        }

    fun mapHovedytelse(hovedytelse: Map<HovedytelseType, Boolean>): List<Hovedytelse> =
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

    fun mapÅrsakOppholdUtenforNorge(arsakOppholdUtenforNorge: Map<ArsakOppholdUtenforNorgeType, Boolean>): List<ÅrsakOppholdUtenforNorge> =
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
