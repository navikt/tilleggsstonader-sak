package no.nav.tilleggsstonader.sak.behandling.opprettelse.dummy

import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.søknad.InnsendtSkjema
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaDagligReiseFyllUtSendInn
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.AktivitetMetadata
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Aktiviteter
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.AktiviteterMetadata
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.AktiviteterOgMålgruppeMetadata
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.ArbeidsrettetAktivitetType
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.DagligReiseFyllUtSendInnData
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.DataFetcher
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.DineOpplysninger
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.FaktiskeUtgifter
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.GarDuPaVideregaendeEllerGrunnskoleType
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
import no.nav.tilleggsstonader.libs.utils.dato.april
import no.nav.tilleggsstonader.libs.utils.dato.august
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import org.springframework.stereotype.Service
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
                        gyldigFraOgMed = 8 august 2001,
                        adresse = "Repsetvegen 100",
                        postnummer = "2230",
                        bySted = "SKOTTERUD",
                        landkode = "NORGE",
                        land =
                            Landvelger(
                                value = "Norge",
                                label = "NORGE",
                            ),
                    ),
                reiseFraFolkeregistrertAdr = JaNeiType.ja,
                adresseJegSkalReiseFra = null,
            )
        val aktiviteter =
            Aktiviteter(
                aktiviteterOgMaalgruppe = mapOf("134124111" to true, "134125430" to true, "annet" to true),
                arbeidsrettetAktivitet = ArbeidsrettetAktivitetType.tiltakArbeidsrettetUtredning,
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

        val reise1 =
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
                syvdagersbillett = null,
                manedskort = null,
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

        val reise2 =
            Reise(
                gateadresse = "Adressen 1",
                postnr = "1100",
                poststed = "Oslo",
                fom = 1 mars 2026,
                tom = 30 april 2026,
                hvorMangeDagerIUkenSkalDuMoteOppPaAktivitetstedet = Valgfelt("dager", "3"),
                harDu6KmReisevei = JaNeiType.ja,
                harDuAvMedisinskeArsakerBehovForTransportUavhengigAvReisensLengde = null,
                hvorLangErReiseveienDin = 9.0,
                kanDuReiseMedOffentligTransport = JaNeiType.ja,
                hvaSlagsTypeBillettMaDuKjope =
                    mapOf(
                        HvaSlagsTypeBillettMaDuKjopeType.manedskort to true,
                    ),
                enkeltbillett = null,
                syvdagersbillett = null,
                manedskort = 1200,
                hvaErViktigsteGrunnerTilAtDuIkkeKanBrukeOffentligTransport = null,
                kanKjoreMedEgenBil = null,
                skalDuBetaleForReisenSelv = null,
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
                                                    label = "Praksisplass i Indre Syltedalen: 10. september 2025 - 30. juni 2026",
                                                    type = "TILTAK",
                                                ),
                                                AktivitetMetadata(
                                                    value = "134124111",
                                                    label = "Arbeidstrening: 16. juni 2025 - 31. juli 2025",
                                                    type = "TILTAK",
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
                            arbeidOgOpphold = null,
                            aktiviteter = aktiviteter,
                            reise = listOf(reise1, reise2),
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
