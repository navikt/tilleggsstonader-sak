package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaBoutgifterFyllUtSendInn
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.Aktivitet
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.Aktiviteter
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.AktiviteterOgMålgruppe
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.ArbeidsrettetAktivitetType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.BoligEllerOvernatting
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.BoutgifterFyllUtSendInnData
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.DelerBoutgifterType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.DineOpplysninger
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.FasteUtgifter
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.HarUtgifterTilBoligToStederType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.HovedytelseType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.Identitet
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.JaNeiType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.Landvelger
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.NavAdresse
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.PeriodeForSamling
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.Samling
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.SkjemaBoutgifter
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.TypeUtgifterType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.UtgifterFlereSteder
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.UtgifterNyBolig
import java.time.LocalDate

object SøknadBoutgifterUtil {
    fun søknadBoutgifter(): SøknadsskjemaBoutgifterFyllUtSendInn {
        val skjemaBoutgifter =
            SkjemaBoutgifter(
                dineOpplysninger = dineOpplysninger(),
                hovedytelse = mapOf(HovedytelseType.arbeidsavklaringspenger to true),
                harNedsattArbeidsevne = JaNeiType.ja,
                arbeidOgOpphold = null,
                aktiviteter = aktiviteter(),
                boligEllerOvernatting = boligEllerOvernatting(),
            )
        return SøknadsskjemaBoutgifterFyllUtSendInn(
            language = "nb-NO",
            data = BoutgifterFyllUtSendInnData(data = skjemaBoutgifter),
            dokumentasjon = emptyList(),
        )
    }

    private fun boligEllerOvernatting(): BoligEllerOvernatting =
        BoligEllerOvernatting(
            typeUtgifter = TypeUtgifterType.midlertidigUtgift,
            fasteUtgifter = fasteUtgifter(),
            samling = samling(),
            harSaerligStoreUtgifterPaGrunnAvFunksjonsnedsettelse = JaNeiType.nei,
        )

    private fun fasteUtgifter(): FasteUtgifter =
        FasteUtgifter(
            harUtgifterTilBoligToSteder = HarUtgifterTilBoligToStederType.ekstraBolig,
            utgifterFlereSteder =
                UtgifterFlereSteder(
                    delerBoutgifter = mapOf(DelerBoutgifterType.aktivitetssted to true),
                    andelUtgifterBoligHjemsted = 1300,
                    andelUtgifterBoligAktivitetssted = 1000,
                    harLeieinntekter = JaNeiType.ja,
                    leieinntekterPerManed = 1000,
                ),
            utgifterNyBolig =
                UtgifterNyBolig(
                    delerBoutgifter = JaNeiType.ja,
                    andelUtgifterBolig = 900,
                    harHoyereUtgifterPaNyttBosted = JaNeiType.ja,
                    mottarBostotte = JaNeiType.nei,
                ),
        )

    private fun samling(): Samling =
        Samling(
            periodeForSamling =
                listOf(
                    PeriodeForSamling(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 1),
                        trengteEkstraOvernatting = JaNeiType.nei,
                        utgifterTilOvernatting = 1000,
                    ),
                ),
        )

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
                            value = "Norge",
                            label = "NO",
                        ),
                ),
        )

    private fun aktiviteter(): Aktiviteter =
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
}
