package no.nav.tilleggsstonader.sak.behandling.opprettelse.dummy

import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.søknad.InnsendtSkjema
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaBoutgifterFyllUtSendInn
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.Aktivitet
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.Aktiviteter
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.AktiviteterOgMålgruppe
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.ArbeidOgOpphold
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
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.OppholdUtenforNorge
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.Periode
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.PeriodeForSamling
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.Samling
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.SkjemaBoutgifter
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.TypeUtgifterType
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.UtgifterFlereSteder
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.UtgifterNyBolig
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.november
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class OpprettDummySøknadBoutgifter(
    private val søknadService: SøknadService,
) {
    fun opprettDummy(
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
                harSaerligStoreUtgifterPaGrunnAvFunksjonsnedsettelse = JaNeiType.ja,
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
            InnsendtSkjema(
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
}

private fun arbeidOgOppholdBoutgifter() =
    ArbeidOgOpphold(
        jobberIAnnetLand = JaNeiType.ja,
        jobbAnnetLand = Landvelger("SWE", "Sverige"),
        harPengestotteAnnetLand = mapOf(HarPengestotteAnnetLandType.sykepenger to true),
        pengestotteAnnetLand = Landvelger("SWE", "Sverige"),
        harOppholdUtenforNorgeSiste12mnd = JaNeiType.ja,
        oppholdUtenforNorgeSiste12mnd =
            OppholdUtenforNorge(
                Landvelger("SWE", "Sverige"),
                mapOf(ArsakOppholdUtenforNorgeType.besokteFamilie to true),
                Periode(1 januar 2024, 10 januar 2024),
            ),
        harOppholdUtenforNorgeNeste12mnd = JaNeiType.ja,
        oppholdUtenforNorgeNeste12mnd =
            OppholdUtenforNorge(
                Landvelger("SWE", "Sverige"),
                mapOf(ArsakOppholdUtenforNorgeType.besokteFamilie to true),
                Periode(1 januar 2024, 30 november 2024),
            ),
    )
