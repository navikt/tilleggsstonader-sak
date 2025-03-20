package no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaBoutgifterFyllUtSendInn
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.AnnenAktivitetType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.ArsakOppholdUtenforNorge
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.BoligEllerOvernatting
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.Samling
import no.nav.tilleggsstonader.kontrakter.søknad.felles.ÅrsakOppholdUtenforNorge
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.AktivitetAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.ArbeidOgOpphold
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.OppholdUtenforNorge
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.ValgtAktivitet
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.ArbeidOgOpphold as ArbeidOgOppholdKontrakt
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.FasteUtgifter as FasteUtgifterKontrakt
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.Hovedytelse as HovedytelseKontrakt
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.OppholdUtenforNorge as OppholdUtenforNorgeKontrakt
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.SkjemaBoutgifter as SkjemaBoutgifterKontrakt

object SøknadskjemaBoutgifterMapper {
    fun mapSkjema(boutgifter: SøknadsskjemaBoutgifterFyllUtSendInn): SkjemaBoutgifter {
        val skjemaBoutgifter = boutgifter.data.data
        return mapSkjema(skjemaBoutgifter)
    }

    fun mapSkjema(skjemaBoutgifter: SkjemaBoutgifterKontrakt): SkjemaBoutgifter =
        SkjemaBoutgifter(
            hovedytelse = mapHovedytelse(skjemaBoutgifter),
            aktivitet = mapAktivitet(skjemaBoutgifter),
            boutgifter = mapBoutgifter(skjemaBoutgifter.boligEllerOvernatting),
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
        return AktivitetAvsnitt(
            aktiviteter = listOf(ValgtAktivitet(id = aktivitet.aktivitetId, label = aktivitet.text)),
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

    fun mapHovedytelse(hovedytelse: HovedytelseKontrakt): List<Hovedytelse> =
        buildList {
            val list = this
            with(hovedytelse) {
                list.add(arbeidsavklaringspenger, Hovedytelse.AAP)
                list.add(overgangsstonad, Hovedytelse.OVERGANGSSTØNAD)
                list.add(gjenlevendepensjon, Hovedytelse.GJENLEVENDEPENSJON)
                list.add(uforetrygd, Hovedytelse.UFØRETRYGD)
                list.add(tiltakspenger, Hovedytelse.TILTAKSPENGER)
                list.add(dagpenger, Hovedytelse.DAGPENGER)
                list.add(sykepenger, Hovedytelse.SYKEPENGER)
                list.add(kvalifiseringsstonad, Hovedytelse.KVALIFISERINGSSTØNAD)
                list.add(mottarIngenPengestotte, Hovedytelse.INGEN_PENGESTØTTE)
                list.add(ingenAvAlternativenePasserForMeg, Hovedytelse.INGEN_PASSENDE_ALTERNATIVER)
            }
        }

    fun mapÅrsakOppholdUtenforNorge(arsakOppholdUtenforNorge: ArsakOppholdUtenforNorge): List<ÅrsakOppholdUtenforNorge> =
        buildList {
            val list = this
            with(arsakOppholdUtenforNorge) {
                list.add(jobbet, ÅrsakOppholdUtenforNorge.JOBB)
                list.add(studerte, ÅrsakOppholdUtenforNorge.STUDIER)
                list.add(fikkMedisinskBehandling, ÅrsakOppholdUtenforNorge.MEDISINSK_BEHANDLING)
                list.add(varPaFerie, ÅrsakOppholdUtenforNorge.FERIE)
                list.add(besokteFamilie, ÅrsakOppholdUtenforNorge.FAMILIE_BESØK)
                list.add(annet, ÅrsakOppholdUtenforNorge.ANNET)
            }
        }

    private fun <T> MutableList<T>.add(
        boolean: Boolean,
        verdi: T,
    ) {
        if (boolean) {
            add(verdi)
        }
    }
}
