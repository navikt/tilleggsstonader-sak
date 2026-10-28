package no.nav.tilleggsstonader.sak.behandling.opprettelse.dummy

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.søknad.DatoFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFlereValgFelt
import no.nav.tilleggsstonader.kontrakter.søknad.InnsendtSkjema
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.SelectFelt
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaROAH
import no.nav.tilleggsstonader.kontrakter.søknad.VerdiFelt
import no.nav.tilleggsstonader.kontrakter.søknad.felles.HovedytelseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.felles.ReisemåteAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.OffentligTransportInfo
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.Transportmiddel
import no.nav.tilleggsstonader.kontrakter.søknad.roah.AdresseMidlertidigBostedAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.roah.AdresseOriginaltBostedAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.roah.AdresseOriginaltBostedDetaljerAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.roah.PeriodeMidlertidigBostedAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.roah.ROAHAdresseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.roah.ROAHAktivitetAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.roah.ROAHBarnOgSærligeBehovAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.roah.ROAHReiseInfo
import no.nav.tilleggsstonader.kontrakter.søknad.roah.ROAHTypeReise
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.november
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OpprettDummySøknadROAH(
    private val søknadService: SøknadService,
) {
    fun opprettDummy(
        fagsak: Fagsak,
        behandling: Behandling,
    ) {
        val reisemåte =
            ReisemåteAvsnitt(
                hvilkeTransportmidlerBleBenyttet =
                    EnumFlereValgFelt(
                        label = "Hvilke transportmidler brukte du?",
                        verdier = listOf(VerdiFelt(Transportmiddel.OFFENTLIG_TRANSPORT, "Offentlig transport")),
                        alternativer = emptyList(),
                    ),
                unntakFraOffentligTransport = null,
                unntakFraPrivatBil = null,
                offentligTransport =
                    OffentligTransportInfo(
                        totalUtgifterOffentligTransport = VerdiFelt("1200", "Reiseutgifter"),
                    ),
                privatBil = null,
                drosje = null,
            )
        val skjema =
            SøknadsskjemaROAH(
                hovedytelse =
                    HovedytelseAvsnitt(
                        hovedytelse =
                            EnumFlereValgFelt(
                                label = "Hovedytelse",
                                verdier = listOf(VerdiFelt(Hovedytelse.AAP, "Arbeidsavklaringspenger (AAP)")),
                                alternativer = emptyList(),
                            ),
                        arbeidOgOpphold = null,
                    ),
                aktivitet =
                    ROAHAktivitetAvsnitt(
                        aktiviteter =
                            EnumFlereValgFelt(
                                label = "Aktivitet",
                                verdier = listOf(VerdiFelt("ARBTREN", "Arbeidstrening")),
                                alternativer = emptyList(),
                            ),
                        annenAktivitet = null,
                        lønnetAktivitet = EnumFelt("Har du lønnet aktivitet?", JaNei.NEI, "Nei", emptyList()),
                    ),
                adresser =
                    ROAHAdresseAvsnitt(
                        måBoBorteHjemmefra = EnumFelt("Må du bo borte hjemmefra?", JaNei.JA, "Ja", emptyList()),
                        periodeMidlertidigBosted =
                            PeriodeMidlertidigBostedAvsnitt(
                                fomFlyttedato = DatoFelt("Fra dato", 3 november 2026),
                                tomFlyttedato = DatoFelt("Til dato", 30 november 2026),
                            ),
                        adresseOriginaltBosted =
                            AdresseOriginaltBostedAvsnitt(
                                skalReiseFraFolkeregistrertAdresse =
                                    EnumFelt("Skal du reise fra folkeregistrert adresse?", JaNei.NEI, "Nei", emptyList()),
                                adresseOriginaltBosted =
                                    AdresseOriginaltBostedDetaljerAvsnitt(
                                        land = SelectFelt("Land", "NOR", "Norge"),
                                        gateadresse = VerdiFelt("Testgata 1", "Gateadresse"),
                                        postnummer = VerdiFelt("0150", "Postnummer"),
                                        poststed = VerdiFelt("Oslo", "Poststed"),
                                    ),
                            ),
                        adresseMidlertidigBosted =
                            AdresseMidlertidigBostedAvsnitt(
                                land = SelectFelt("Land", "NOR", "Norge"),
                                gateadresse = VerdiFelt("Eksempelveien 2", "Gateadresse"),
                                postnummer = VerdiFelt("5003", "Postnummer"),
                                poststed = VerdiFelt("Bergen", "Poststed"),
                            ),
                    ),
                barnOgSærligeBehov =
                    ROAHBarnOgSærligeBehovAvsnitt(
                        harBarnUnder18SomHarFlyttetMed = EnumFelt("Har barn under 18 år flyttet med?", JaNei.NEI, "Nei", emptyList()),
                        hvilkeBarnFlytterMed = null,
                        harBarnHjemmeUnder4Klasse = EnumFelt("Har du barn hjemme under 4. klasse?", JaNei.JA, "Ja", emptyList()),
                        harSærligeBehovForFlereHjemreiser =
                            EnumFelt("Har du særlige behov for flere hjemreiser?", JaNei.NEI, "Nei", emptyList()),
                    ),
                reiser =
                    listOf(
                        ROAHReiseInfo(
                            typeReise = EnumFelt("Type reise", ROAHTypeReise.OPPSTART, "Oppstart", emptyList()),
                            fom = DatoFelt("Fra og med", 3 november 2026),
                            reisemåte = reisemåte,
                        ),
                        ROAHReiseInfo(
                            typeReise = EnumFelt("Type reise", ROAHTypeReise.AVSLUTNING, "Avslutning", emptyList()),
                            fom = DatoFelt("Fra og med", 30 november 2026),
                            tom = DatoFelt("Til og med", 30 november 2026),
                            reisemåte = reisemåte,
                        ),
                        ROAHReiseInfo(
                            typeReise = EnumFelt("Type reise", ROAHTypeReise.HJEMREISE, "Hjemreise", emptyList()),
                            fom = DatoFelt("Fra og med", 1 desember 2026),
                            reisemåte = reisemåte,
                        ),
                    ),
                dokumentasjon = emptyList(),
            )
        val innsendtSkjema =
            InnsendtSkjema(
                ident = fagsak.hentAktivIdent(),
                mottattTidspunkt = LocalDateTime.now(),
                språk = Språkkode.NB,
                skjema = skjema,
            )
        val journalpost = Journalpost("TESTJPID", Journalposttype.I, Journalstatus.FERDIGSTILT)
        søknadService.lagreSøknad(behandling.id, journalpost, innsendtSkjema)
    }
}
