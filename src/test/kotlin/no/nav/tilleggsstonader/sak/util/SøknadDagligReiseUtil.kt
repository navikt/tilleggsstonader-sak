package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaDagligReiseFyllUtSendInn
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.AktivitetMetadata
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Aktiviteter
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.AktiviteterMetadata
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.AktiviteterOgMålgruppeMetadata
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.ArbeidOgOpphold
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.DagligReiseFyllUtSendInnData
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.DataFetcher
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.DineOpplysninger
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.FaktiskeUtgifter
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.GarDuPaVideregaendeEllerGrunnskoleType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HarPengestotteAnnetLandType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HovedytelseType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HvaSlagsTypeBillettMaDuKjopeType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Identitet
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.JaNeiType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Landvelger
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.MetadataDagligReise
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.NavAdresse
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Reise
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.SkjemaDagligReise
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Valgfelt
import no.nav.tilleggsstonader.libs.utils.dato.januar
import java.time.LocalDate

object SøknadDagligReiseUtil {
    fun søknadDagligReise(hovedytelse: Map<HovedytelseType, Boolean> = defaultHovedytelse): SøknadsskjemaDagligReiseFyllUtSendInn {
        val skjemaDagligReise =
            SkjemaDagligReise(
                jegSokerPaVegneAvMegSelv = true,
                dineOpplysninger = dineOpplysninger(),
                hovedytelse = hovedytelse,
                aktiviteter = aktiviteter(),
                reise = listOf(reise()),
                arbeidOgOpphold =
                    ArbeidOgOpphold(
                        jobberIAnnetLand = JaNeiType.nei,
                        jobbAnnetLand = null,
                        harPengestotteAnnetLand = mapOf(HarPengestotteAnnetLandType.mottarIkkePengestotte to true),
                        pengestotteAnnetLand = null,
                        harOppholdUtenforNorgeNeste12mnd = JaNeiType.nei,
                        oppholdUtenforNorgeNeste12mnd = null,
                        harOppholdUtenforNorgeSiste12mnd = JaNeiType.nei,
                        oppholdUtenforNorgeSiste12mnd = null,
                    ),
            )
        return SøknadsskjemaDagligReiseFyllUtSendInn(
            language = "nb-NO",
            data = DagligReiseFyllUtSendInnData(skjemaDagligReise, metadata()),
            dokumentasjon = emptyList(),
        )
    }

    private val defaultHovedytelse = mapOf(HovedytelseType.arbeidsavklaringspenger to true)

    private fun dineOpplysninger(): DineOpplysninger =
        DineOpplysninger(
            fornavn = "Fornavn",
            etternavn = "Etternavn",
            identitet =
                Identitet(
                    identitetsnummer = "11111122222",
                ),
            adresse =
                NavAdresse(
                    gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                    adresse = "Nisseveien 3",
                    postnummer = "0011",
                    bySted = "OSLO",
                    landkode = "NO",
                    land =
                        Landvelger(
                            label = "Norge",
                            value = "NO",
                        ),
                ),
            reiseFraFolkeregistrertAdr = JaNeiType.nei,
            adresseJegSkalReiseFra = null,
        )

    private fun aktiviteter(): Aktiviteter =
        Aktiviteter(
            aktiviteterOgMaalgruppe =
                mapOf(
                    "134125430" to true,
                    "134124111" to false,
                    "annet" to false,
                ),
            arbeidsrettetAktivitet = null,
            faktiskeUtgifter =
                FaktiskeUtgifter(
                    garDuPaVideregaendeEllerGrunnskole = GarDuPaVideregaendeEllerGrunnskoleType.annetTiltak,
                    erDuLaerling = null,
                    arbeidsgiverDekkerUtgift = null,
                    under25 = null,
                    betalerForReisenTilSkolenSelv = null,
                    lonnGjennomTiltak = null,
                ),
        )

    private fun reise(): Reise =
        Reise(
            gateadresse = "Eksempelveien 1",
            postnr = "0123",
            poststed = "OSLO",
            fom = 1 januar 2025,
            tom = 31 januar 2025,
            hvorMangeDagerIUkenSkalDuMoteOppPaAktivitetstedet =
                Valgfelt(
                    label = "5",
                    value = "5",
                ),
            harDu6KmReisevei = JaNeiType.ja,
            hvorLangErReiseveienDin = 15.0,
            harDuAvMedisinskeArsakerBehovForTransportUavhengigAvReisensLengde = JaNeiType.nei,
            kanDuReiseMedOffentligTransport = JaNeiType.ja,
            hvaSlagsTypeBillettMaDuKjope =
                mapOf(
                    HvaSlagsTypeBillettMaDuKjopeType.manedskort to true,
                ),
            enkeltbillett = null,
            syvdagersbillett = null,
            manedskort = 800,
            kanKjoreMedEgenBil = JaNeiType.nei,
            hvaErViktigsteGrunnerTilAtDuIkkeKanBrukeOffentligTransport = null,
            mottarDuGrunnstonadFraNav = null,
            hvorforIkkeBil = null,
            reiseMedTaxi = null,
            ttKort = null,
            parkering = null,
            bompenger = null,
            ferge = null,
            piggdekkavgift = null,
        )

    private fun metadata(): MetadataDagligReise =
        MetadataDagligReise(
            dataFetcher =
                DataFetcher(
                    aktiviteter =
                        AktiviteterMetadata(
                            aktiviteterOgMaalgruppe =
                                AktiviteterOgMålgruppeMetadata(
                                    data =
                                        listOf(
                                            AktivitetMetadata(
                                                value = "134125430",
                                                label = "Høyere utdanning: 10. september 2025 - 30. juni 2026",
                                                type = "TILTAK",
                                            ),
                                            AktivitetMetadata(
                                                value = "134125430",
                                                label = "Høyere utdanning: 10. september 2025 - 30. juni 2026",
                                                type = "TILTAK",
                                            ),
                                            AktivitetMetadata(
                                                value = "134125430",
                                                label = "Høyere utdanning: 10. september 2025 - 30. juni 2026",
                                                type = "TILTAK",
                                            ),
                                        ),
                                ),
                        ),
                ),
        )
}
