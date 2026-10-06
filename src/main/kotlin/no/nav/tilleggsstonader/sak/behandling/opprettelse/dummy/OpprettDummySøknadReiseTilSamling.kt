package no.nav.tilleggsstonader.sak.behandling.opprettelse.dummy

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariant
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.søknad.DatoFelt
import no.nav.tilleggsstonader.kontrakter.søknad.Dokument
import no.nav.tilleggsstonader.kontrakter.søknad.DokumentasjonFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFlereValgFelt
import no.nav.tilleggsstonader.kontrakter.søknad.InnsendtSkjema
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.SelectFelt
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaReiseTilSamling
import no.nav.tilleggsstonader.kontrakter.søknad.Vedleggstype
import no.nav.tilleggsstonader.kontrakter.søknad.VerdiFelt
import no.nav.tilleggsstonader.kontrakter.søknad.felles.AnnenAktivitetType
import no.nav.tilleggsstonader.kontrakter.søknad.felles.HovedytelseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.Adresse
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.AktivitetTypeUtdanning
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.AvreiseadresseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.DrivstoffType
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.DrosjeInfo
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.InfoBilKunDelerAvStrekning
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.OffentligTransportInfo
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.PrivatBilInfo
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.ReiseTilSamlingAktivitetAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.ReisemåteAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.Samling
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.TilleggsopplysningerAnnenAktivitetAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.Transportmiddel
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.UnntakFraOffentligTransport
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.UtgifterPrivatBil
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.ÅrsakKanIkkeBenytteEgenBil
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.ÅrsakKanIkkeBenytteOffentligTransport
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class OpprettDummySøknadReiseTilSamling(
    private val søknadService: SøknadService,
) {
    fun opprettDummy(
        fagsak: Fagsak,
        behandling: Behandling,
    ) {
        val vedleggBekreftelseSamlingerId = UUID.randomUUID()
        val søknadVedleggId = UUID.randomUUID()

        val hovedytelse =
            HovedytelseAvsnitt(
                hovedytelse =
                    EnumFlereValgFelt(
                        "",
                        listOf(VerdiFelt(Hovedytelse.AAP, "AAP")),
                        emptyList(),
                    ),
                arbeidOgOpphold = null,
            )
        val aktivitet =
            ReiseTilSamlingAktivitetAvsnitt(
                aktiviteter =
                    EnumFlereValgFelt(
                        label = "Hvilken aktivitet søker du støtte til?",
                        verdier =
                            listOf(
                                VerdiFelt("1", "Tiltak: 12. februar 2026 - 12. mars 2026"),
                            ),
                        alternativer = listOf("Tiltak: 12. februar 2026 - 12. mars 2026"),
                    ),
                annenAktivitet =
                    EnumFelt(
                        label = "Hvilken arbeidsrettet aktivitet har du?",
                        verdi = AnnenAktivitetType.TILTAK,
                        svarTekst = "Tiltak / arbeidsrettet aktivitet",
                        alternativer = emptyList(),
                    ),
                lønnetAktivitet =
                    EnumFelt(
                        label = "Mottar du ordinær lønn gjennom tiltaket?",
                        verdi = JaNei.JA,
                        svarTekst = "Ja",
                        alternativer = emptyList(),
                    ),
                tilleggsopplysningerAnnenAktivitet =
                    TilleggsopplysningerAnnenAktivitetAvsnitt(
                        erLærlingEllerLiknende =
                            EnumFelt(
                                label =
                                    "Er du lærling, lærekandidat, praksisbrevkandidat eller kandidat for fagbrev på jobb?",
                                verdi = JaNei.NEI,
                                svarTekst = "Nei",
                                alternativer = emptyList(),
                            ),
                        fårDekketReise =
                            EnumFelt(
                                label = "Får du dekket reisen til aktivitetsstedet av arbeidsgiveren din?",
                                verdi = JaNei.NEI,
                                svarTekst = "Nei",
                                alternativer = emptyList(),
                            ),
                        erUnder25År =
                            EnumFelt(
                                label = "Er eller var du under 25 år ved starten av skoleåret?",
                                verdi = JaNei.NEI,
                                svarTekst = "Nei",
                                alternativer = emptyList(),
                            ),
                        måBetaleForReiseTilSkole =
                            EnumFelt(
                                label = "Må du betale for reisen til skolen selv?",
                                verdi = JaNei.NEI,
                                svarTekst = "Nei",
                                alternativer = emptyList(),
                            ),
                    ),
                annenAktivitetTypeUtdanning =
                    EnumFelt(
                        label = "Hva slags type arbeidsrettet aktivitet går du på?",
                        verdi = AktivitetTypeUtdanning.OPPLÆRING_FOR_VOKSNE,
                        svarTekst = "Forberedende opplæring for voksne",
                        alternativer = emptyList(),
                    ),
            )
        val samling1 = // Samling 1: kun offentlig transport
            Samling(
                fom = DatoFelt("Fra", 12 februar 2026),
                tom = DatoFelt("Til", 14 februar 2026),
                erObligatorisk =
                    EnumFelt(
                        label = "Er samlingen obligatorisk?",
                        verdi = JaNei.JA,
                        svarTekst = "Ja",
                        alternativer = emptyList(),
                    ),
                adresse =
                    Adresse(
                        land = SelectFelt("Land", "NO", "Norge"),
                        gateadresse = VerdiFelt(verdi = "Mimes vei 1", label = "Gateadresse"),
                        postnummer = VerdiFelt(verdi = "5132", label = "Postnummer"),
                        poststed = VerdiFelt(verdi = "Nyborg", label = "Poststed"),
                    ),
                antallKilometerEnVei = VerdiFelt(verdi = "42", label = "Antall kilometer én vei"),
                reisemåte =
                    ReisemåteAvsnitt(
                        hvilkeTransportmidlerBleBenyttet =
                            EnumFlereValgFelt(
                                label = "Hvilke transportmidler ble benyttet?",
                                verdier =
                                    listOf(
                                        VerdiFelt(
                                            Transportmiddel.OFFENTLIG_TRANSPORT,
                                            "Offentlig transport",
                                        ),
                                    ),
                                alternativer = emptyList(),
                            ),
                        unntakFraOffentligTransport = null,
                        unntakFraPrivatBil = null,
                        offentligTransport =
                            OffentligTransportInfo(
                                totalUtgifterOffentligTransport =
                                    VerdiFelt(
                                        verdi = "890",
                                        label = "Totale utgifter til offentlig transport",
                                    ),
                            ),
                        privatBil = null,
                        drosje = null,
                    ),
            )
        val samling2 = // Samling 2: kombinasjon av offentlig transport og privat bil
            Samling(
                fom = DatoFelt("Fra", 10 mars 2026),
                tom = DatoFelt("Til", 12 mars 2026),
                erObligatorisk =
                    EnumFelt(
                        label = "Er samlingen obligatorisk?",
                        verdi = JaNei.JA,
                        svarTekst = "Ja",
                        alternativer = emptyList(),
                    ),
                adresse =
                    Adresse(
                        land = SelectFelt("Land", "NO", "Norge"),
                        gateadresse = VerdiFelt(verdi = "Mimes vei 1", label = "Gateadresse"),
                        postnummer = VerdiFelt(verdi = "5132", label = "Postnummer"),
                        poststed = VerdiFelt(verdi = "Nyborg", label = "Poststed"),
                    ),
                antallKilometerEnVei = VerdiFelt(verdi = "42", label = "Antall kilometer én vei"),
                reisemåte =
                    ReisemåteAvsnitt(
                        hvilkeTransportmidlerBleBenyttet =
                            EnumFlereValgFelt(
                                label = "Hvilke transportmidler ble benyttet?",
                                verdier =
                                    listOf(
                                        VerdiFelt(
                                            Transportmiddel.OFFENTLIG_TRANSPORT,
                                            "Offentlig transport",
                                        ),
                                        VerdiFelt(
                                            Transportmiddel.PRIVAT_BIL,
                                            "Privat bil",
                                        ),
                                    ),
                                alternativer = emptyList(),
                            ),
                        unntakFraOffentligTransport =
                            UnntakFraOffentligTransport(
                                årsaker =
                                    EnumFlereValgFelt(
                                        label = "Hvorfor kan du ikke bruke offentlig transport?",
                                        verdier =
                                            listOf(
                                                VerdiFelt(
                                                    ÅrsakKanIkkeBenytteOffentligTransport
                                                        .DÅRLIG_TRANSPORTTILBUD,
                                                    "Dårlig transporttilbud",
                                                ),
                                            ),
                                        alternativer = emptyList(),
                                    ),
                                leveringOgHentingIBarnehage = null,
                            ),
                        unntakFraPrivatBil = null,
                        offentligTransport =
                            OffentligTransportInfo(
                                totalUtgifterOffentligTransport =
                                    VerdiFelt(
                                        verdi = "450",
                                        label = "Totale utgifter til offentlig transport",
                                    ),
                            ),
                        privatBil =
                            PrivatBilInfo(
                                benyttetEgenBil =
                                    EnumFelt(
                                        label = "Benyttet du egen bil?",
                                        verdi = JaNei.JA,
                                        svarTekst = "Ja",
                                        alternativer = emptyList(),
                                    ),
                                betalteForReisen =
                                    EnumFelt(
                                        label = "Betalte du for reisen?",
                                        verdi = JaNei.JA,
                                        svarTekst = "Ja",
                                        alternativer = emptyList(),
                                    ),
                                infoBilKunDelerAvStrekning =
                                    InfoBilKunDelerAvStrekning(
                                        strekningHvorBilBleBenyttet =
                                            VerdiFelt(
                                                verdi = "Fra hjemmet til togstasjonen",
                                                label = "Hvilken del av reisen ble kjørt med privat bil?",
                                            ),
                                        antallKilometerKjørt =
                                            VerdiFelt(
                                                verdi = "15",
                                                label = "Hvor mange kilometer kjørte du totalt med privat bil?",
                                            ),
                                    ),
                                utgifterPrivatBil =
                                    UtgifterPrivatBil(
                                        bompenger = VerdiFelt(verdi = "150", label = "Bompenger"),
                                        ferge = VerdiFelt(verdi = "0", label = "Ferge"),
                                        piggdekkavgift =
                                            VerdiFelt(verdi = "60", label = "Piggdekkavgift"),
                                        parkering = VerdiFelt(verdi = "123", label = "Parkering"),
                                        drivstoffType =
                                            EnumFelt(
                                                label = "Drivstofftype",
                                                verdi = DrivstoffType.ELBIL,
                                                svarTekst = "Elbil",
                                                alternativer = emptyList(),
                                            ),
                                    ),
                            ),
                        drosje = null,
                    ),
            )
        val samling3 = // Samling 3: drosje, med unntak for privat bil
            Samling(
                fom = DatoFelt("Fra", 1 januar 2026),
                tom = DatoFelt("Til", 3 januar 2026),
                erObligatorisk =
                    EnumFelt(
                        label = "Er samlingen obligatorisk?",
                        verdi = JaNei.JA,
                        svarTekst = "Ja",
                        alternativer = emptyList(),
                    ),
                adresse =
                    Adresse(
                        land = SelectFelt("Land", "NO", "Norge"),
                        gateadresse = VerdiFelt(verdi = "Bilveien 1", label = "Gateadresse"),
                        postnummer = VerdiFelt(verdi = "1100", label = "Postnummer"),
                        poststed = VerdiFelt(verdi = "Oslo", label = "Poststed"),
                    ),
                antallKilometerEnVei = VerdiFelt(verdi = "120", label = "Antall kilometer én vei"),
                reisemåte =
                    ReisemåteAvsnitt(
                        hvilkeTransportmidlerBleBenyttet =
                            EnumFlereValgFelt(
                                label = "Hvilke transportmidler ble benyttet?",
                                verdier =
                                    listOf(
                                        VerdiFelt(
                                            Transportmiddel.DROSJE,
                                            "Drosje",
                                        ),
                                    ),
                                alternativer = emptyList(),
                            ),
                        unntakFraOffentligTransport = null,
                        unntakFraPrivatBil =
                            EnumFlereValgFelt(
                                label = "Hvorfor kan du ikke bruke privat bil?",
                                verdier =
                                    listOf(
                                        VerdiFelt(
                                            ÅrsakKanIkkeBenytteEgenBil
                                                .HAR_IKKE_BIL_ELLER_FØRERKORT,
                                            "Har ikke bil eller førerkort",
                                        ),
                                    ),
                                alternativer = emptyList(),
                            ),
                        offentligTransport = null,
                        privatBil = null,
                        drosje =
                            DrosjeInfo(
                                EnumFelt(
                                    label = "Har du TT-kort?",
                                    verdi = JaNei.NEI,
                                    svarTekst = "Nei",
                                    alternativer = emptyList(),
                                ),
                            ),
                    ),
            )
        val avreiseadresse =
            AvreiseadresseAvsnitt(
                skalReiseFraFolkeregistrertAdresse =
                    EnumFelt(
                        label = "Reiser du fra din folkeregistrerte adresse?",
                        verdi = JaNei.NEI,
                        svarTekst = "Nei",
                        alternativer = emptyList(),
                    ),
                adresseDetSkalReisesFra =
                    Adresse(
                        land = SelectFelt("Land", "NO", "Norge"),
                        gateadresse = VerdiFelt(verdi = "Sommerveien 7", label = "Gateadresse"),
                        postnummer = VerdiFelt(verdi = "4621", label = "Postnummer"),
                        poststed = VerdiFelt(verdi = "Kristiansand S", label = "Poststed"),
                    ),
            )
        val skjema =
            SøknadsskjemaReiseTilSamling(
                hovedytelse = hovedytelse,
                aktivitet = aktivitet,
                samlinger =
                    listOf(
                        samling1,
                        samling2,
                        samling3,
                    ),
                avreiseadresse = avreiseadresse,
                dokumentasjon =
                    listOf(
                        DokumentasjonFelt(
                            type = Vedleggstype.BEKREFTELSE_SAMLINGER,
                            label = "Bekreftelse på samlinger",
                            opplastedeVedlegg =
                                listOf(
                                    Dokument(
                                        id = vedleggBekreftelseSamlingerId,
                                        navn = "bekreftelse-samlinger.pdf",
                                    ),
                                ),
                        ),
                    ),
            )
        val skjemaReiseTilSamling =
            InnsendtSkjema(
                ident = fagsak.hentAktivIdent(),
                mottattTidspunkt = LocalDateTime.now(),
                språk = Språkkode.NB,
                skjema = skjema,
            )

        val (tittel, brevkode) =
            when (fagsak.stønadstype) {
                Stønadstype.REISE_TIL_SAMLING_TSO ->
                    "Søknad om reise til samling tso" to "REISE_TIL_SAMLING_TSO"

                Stønadstype.REISE_TIL_SAMLING_TSR ->
                    "Søknad om reise til samling tsr" to "REISE_TIL_SAMLING_TSR"

                else -> error("Ugyldig stønadstype for reise til samling: ${fagsak.stønadstype}")
            }

        val journalpost =
            Journalpost(
                journalpostId = "TESTJPID",
                journalposttype = Journalposttype.I,
                journalstatus = Journalstatus.FERDIGSTILT,
                dokumenter =
                    listOf(
                        DokumentInfo(
                            dokumentInfoId = "DokumentInfoId",
                            tittel = tittel,
                            brevkode = brevkode,
                            dokumentvarianter =
                                listOf(
                                    Dokumentvariant(
                                        variantformat = Dokumentvariantformat.ARKIV,
                                        filnavn = søknadVedleggId.toString(),
                                        saksbehandlerHarTilgang = true,
                                    ),
                                ),
                        ),
                        DokumentInfo(
                            dokumentInfoId = "VedleggDokumentInfoId",
                            tittel = "Bekreftelse på samlinger ",
                            dokumentvarianter =
                                listOf(
                                    Dokumentvariant(
                                        variantformat = Dokumentvariantformat.ARKIV,
                                        filnavn = vedleggBekreftelseSamlingerId.toString(),
                                        saksbehandlerHarTilgang = true,
                                    ),
                                ),
                        ),
                    ),
            )
        søknadService.lagreSøknad(behandling.id, journalpost, skjemaReiseTilSamling)
    }
}
