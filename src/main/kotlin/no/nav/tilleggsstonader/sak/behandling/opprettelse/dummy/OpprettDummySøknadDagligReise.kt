package no.nav.tilleggsstonader.sak.behandling.opprettelse.dummy

import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.søknad.InnsendtSkjema
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaDagligReiseFyllUtSendInn
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.AdresseJegSkalReiseFra
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.AktivitetMetadata
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Aktiviteter
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.AktiviteterMetadata
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.AktiviteterOgMålgruppeMetadata
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.ArbeidOgOpphold
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.ArsakOppholdUtenforNorgeType
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
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.OppholdUtenforNorge
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Periode
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Reise
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.SkjemaDagligReise
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Valgfelt
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.november
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class OpprettDummySøknadDagligReise(
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
                identitet =
                    Identitet(identitetsnummer = "11111122222"),
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
                reiseFraFolkeregistrertAdr = JaNeiType.nei,
                adresseJegSkalReiseFra =
                    AdresseJegSkalReiseFra(
                        gateadresse = "Annen vei 3",
                        postnr = "0482",
                        poststed = "Oslo",
                    ),
            )
        val aktiviteter =
            Aktiviteter(
                aktiviteterOgMaalgruppe = mapOf("134124111" to false, "134125430" to true, "annet" to false),
                arbeidsrettetAktivitet = null,
                faktiskeUtgifter =
                    FaktiskeUtgifter(
                        garDuPaVideregaendeEllerGrunnskole = GarDuPaVideregaendeEllerGrunnskoleType.annetTiltak,
                        erDuLaerling = JaNeiType.ja,
                        arbeidsgiverDekkerUtgift = JaNeiType.ja,
                        under25 = JaNeiType.nei,
                        betalerForReisenTilSkolenSelv = JaNeiType.ja,
                        lonnGjennomTiltak = JaNeiType.ja,
                    ),
            )
        val reise =
            Reise(
                gateadresse = "Nisseveien 3",
                postnr = "0011",
                poststed = "OSLO",
                fom = 1 januar 2025,
                tom = 31 januar 2025,
                hvorMangeDagerIUkenSkalDuMoteOppPaAktivitetstedet = Valgfelt("dager", "5"),
                harDu6KmReisevei = JaNeiType.ja,
                harDuAvMedisinskeArsakerBehovForTransportUavhengigAvReisensLengde = JaNeiType.nei,
                hvorLangErReiseveienDin = 8.0,
                kanDuReiseMedOffentligTransport = JaNeiType.ja,
                hvaSlagsTypeBillettMaDuKjope =
                    mapOf(
                        HvaSlagsTypeBillettMaDuKjopeType.enkeltbillett to true,
                        HvaSlagsTypeBillettMaDuKjopeType.ukeskort to true,
                        HvaSlagsTypeBillettMaDuKjopeType.manedskort to true,
                    ),
                enkeltbillett = 44,
                syvdagersbillett = 280,
                manedskort = 740,
                hvaErViktigsteGrunnerTilAtDuIkkeKanBrukeOffentligTransport = null,
                kanKjoreMedEgenBil = null,
                skalDuBetaleForReisenSelv = JaNeiType.nei,
                mottarDuGrunnstonadFraNav = null,
                hvorforIkkeBil = null,
                reiseMedTaxi = null,
                ttKort = null,
                parkering = null,
                bompenger = null,
                ferge = null,
                piggdekkavgift = null,
            )

        val metadata =
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
                                                    value = "134124111",
                                                    label = "Arbeidstrening: 16. juni 2025 - 31. juli 2025",
                                                    type = "TILTAK",
                                                ),
                                                AktivitetMetadata(
                                                    value = "annet",
                                                    label = "Annet",
                                                    type = null,
                                                ),
                                            ),
                                    ),
                            ),
                    ),
            )

        val skjema =
            SøknadsskjemaDagligReiseFyllUtSendInn(
                language = "nb-NO",
                data =
                    DagligReiseFyllUtSendInnData(
                        SkjemaDagligReise(
                            dineOpplysninger = dineOpplysninger,
                            hovedytelse =
                                mapOf(
                                    HovedytelseType.arbeidsavklaringspenger to
                                        true,
                                ),
                            arbeidOgOpphold = arbeidOgOpphold(),
                            aktiviteter = aktiviteter,
                            reise = listOf(reise),
                        ),
                        metadata = metadata,
                    ),
                dokumentasjon = emptyList(),
            )

        val skjemaDagligReise =
            InnsendtSkjema(
                ident = fagsak.hentAktivIdent(),
                mottattTidspunkt = LocalDateTime.now(),
                språk = Språkkode.NB,
                skjema = skjema,
            )

        val (tittel, brevkode) =
            when (fagsak.stønadstype) {
                Stønadstype.DAGLIG_REISE_TSO ->
                    "Søknad om daglige reiser tso" to "DAGLIG_REISE_TSO"

                Stønadstype.DAGLIG_REISE_TSR ->
                    "Søknad om daglige reiser tsr" to "DAGLIG_REISE_TSR"

                else -> error("Ugyldig stønadstype for daglige reiser: ${fagsak.stønadstype}")
            }

        val journalpost =
            Journalpost(
                "TESTJPID",
                Journalposttype.I,
                Journalstatus.FERDIGSTILT,
                dokumenter =
                    listOf(
                        DokumentInfo(
                            tittel = tittel,
                            dokumentInfoId = "dokumentInfoId",
                            brevkode = brevkode,
                        ),
                    ),
            )
        søknadService.lagreSøknad(behandling.id, journalpost, skjemaDagligReise)
    }
}

private fun arbeidOgOpphold() =
    ArbeidOgOpphold(
        jobberIAnnetLand = JaNeiType.ja,
        jobbAnnetLand =
            Landvelger("SWE", "Sverige"),
        harPengestotteAnnetLand =
            mapOf(
                HarPengestotteAnnetLandType.sykepenger to true,
            ),
        pengestotteAnnetLand =
            Landvelger("SWE", "Sverige"),
        harOppholdUtenforNorgeSiste12mnd = JaNeiType.ja,
        oppholdUtenforNorgeSiste12mnd =
            OppholdUtenforNorge(
                Landvelger("SWE", "Sverige"),
                mapOf(
                    ArsakOppholdUtenforNorgeType.besokteFamilie to true,
                ),
                Periode(1 januar 2024, 10 januar 2024),
            ),
        harOppholdUtenforNorgeNeste12mnd = JaNeiType.ja,
        oppholdUtenforNorgeNeste12mnd =
            OppholdUtenforNorge(
                Landvelger("SWE", "Sverige"),
                mapOf(
                    ArsakOppholdUtenforNorgeType.besokteFamilie to true,
                ),
                Periode(1 januar 2024, 30 november 2024),
            ),
    )
