package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaDagligReiseFyllUtSendInn
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Aktivitet
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Aktiviteter
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.AktiviteterOgMålgruppe
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.ArbeidOgOpphold
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.ArbeidsrettetAktivitetType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.DagligReiseFyllUtSendInnData
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.DineOpplysninger
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Drosje
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HarPengestotteAnnetLandType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HovedytelseType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HvaErViktigsteGrunnerTilAtDuIkkeKanKjoreBilType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HvaSlagsTypeBillettMaDuKjopeType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Identitet
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.JaNeiType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Landvelger
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.NavAdresse
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Reise
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.ReiseAdresse
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Reiseperiode
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.SkjemaDagligReise
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Valgfelt
import java.time.LocalDate

object SøknadDagligReiseUtil {
    fun søknadDagligReise(): SøknadsskjemaDagligReiseFyllUtSendInn {
        val skjemaDagligReise =
            SkjemaDagligReise(
                dineOpplysninger = dineOpplysninger(),
                hovedytelse = mapOf(HovedytelseType.arbeidsavklaringspenger to true),
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
            data = DagligReiseFyllUtSendInnData(skjemaDagligReise),
            dokumentasjon = emptyList(),
        )
    }

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
            reiseTilAktivitetsstedHelePerioden = JaNeiType.ja,
            reiseperiode =
                Reiseperiode(
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 12, 31),
                ),
        )

    private fun reise(): Reise =
        Reise(
            reiseAdresse =
                ReiseAdresse(
                    gateadresse = "Eksempelveien 1",
                    postnr = "0123",
                    poststed = "OSLO",
                ),
            hvorMangeDagerIUkenSkalDuMoteOppPaAktivitetstedet =
                Valgfelt(
                    label = "5",
                    value = "5",
                ),
            harDu6KmReisevei = JaNeiType.ja,
            hvorLangErReiseveienDin = 15,
            harDuAvMedisinskeArsakerBehovForTransportUavhengigAvReisensLengde = JaNeiType.nei,
            kanDuReiseMedOffentligTransport = JaNeiType.ja,
            hvaSlagsTypeBillettMaDuKjope =
                mapOf(
                    HvaSlagsTypeBillettMaDuKjopeType.manedskort to true,
                ),
            enkeltbilett = null,
            syvdagersbilett = null,
            manedskort = 800,
            kanDuKjoreMedEgenBil = JaNeiType.nei,
            utgifterBil = null,
            drosje =
                Drosje(
                    hvaErViktigsteGrunnerTilAtDuIkkeKanKjoreBil =
                        mapOf(
                            HvaErViktigsteGrunnerTilAtDuIkkeKanKjoreBilType.helsemessigeArsaker to true,
                        ),
                    onskerDuASokeOmFaDekketUtgifterTilReiseMedTaxi = JaNeiType.nei,
                ),
            hvaErViktigsteGrunnerTilAtDuIkkeKanBrukeOffentligTransport = null,
        )
}
