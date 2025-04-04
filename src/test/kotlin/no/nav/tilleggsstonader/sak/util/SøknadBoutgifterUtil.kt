package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.søknad.Søknadsskjema
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
import no.nav.tilleggsstonader.libs.utils.osloNow
import java.time.LocalDate
import java.time.LocalDateTime

object SøknadBoutgifterUtil {
    fun søknadskjemaBoutgifter(
        ident: String = "søker",
        mottattTidspunkt: LocalDateTime = osloNow(),
    ): Søknadsskjema<SøknadsskjemaBoutgifterFyllUtSendInn> {
        val dineOpplysninger =
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
        val samling =
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
        val boligEllerOvernatting =
            BoligEllerOvernatting(
                typeUtgifter = TypeUtgifterType.midlertidigUtgift,
                fasteUtgifter =
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
                    ),
                samling = samling,
                harSaerligStoreUtgifterPaGrunnAvFunksjonsnedsettelse = JaNeiType.nei,
            )
        val skjemaBoutgifter =
            SkjemaBoutgifter(
                dineOpplysninger = dineOpplysninger,
                hovedytelse = mapOf(HovedytelseType.arbeidsavklaringspenger to true),
                harNedsattArbeidsevne = JaNeiType.ja,
                arbeidOgOpphold = null,
                aktiviteter =
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
                    ),
                boligEllerOvernatting = boligEllerOvernatting,
            )
        val skjema =
            SøknadsskjemaBoutgifterFyllUtSendInn(
                language = "nb-NO",
                data = BoutgifterFyllUtSendInnData(data = skjemaBoutgifter),
                dokumentasjon = emptyList(),
            )
        return Søknadsskjema(
            ident = ident,
            mottattTidspunkt = mottattTidspunkt,
            språk = Språkkode.NB,
            skjema = skjema,
        )
    }
}
