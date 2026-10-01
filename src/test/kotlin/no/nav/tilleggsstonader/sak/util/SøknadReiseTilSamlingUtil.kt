package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.søknad.DatoFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFlereValgFelt
import no.nav.tilleggsstonader.kontrakter.søknad.InnsendtSkjema
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.SelectFelt
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaReiseTilSamling
import no.nav.tilleggsstonader.kontrakter.søknad.VerdiFelt
import no.nav.tilleggsstonader.kontrakter.søknad.felles.AnnenAktivitetType
import no.nav.tilleggsstonader.kontrakter.søknad.felles.HovedytelseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.Adresse
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.AvreiseadresseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.DrosjeInfo
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.OffentligTransportInfo
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.PrivatBilInfo
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.ReiseTilSamlingAktivitetAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.ReisemåteAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.Samling
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.Transportmiddel
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import java.time.LocalDateTime

object SøknadReiseTilSamlingUtil {
    fun søknadReiseTilSamling(
        ident: String = "11111122222",
        mottattTidspunkt: LocalDateTime = LocalDateTime.of(2026, 2, 1, 12, 0),
    ): InnsendtSkjema<SøknadsskjemaReiseTilSamling> =
        InnsendtSkjema(
            ident = ident,
            mottattTidspunkt = mottattTidspunkt,
            språk = Språkkode.NB,
            skjema =
                SøknadsskjemaReiseTilSamling(
                    hovedytelse =
                        HovedytelseAvsnitt(
                            hovedytelse = EnumFlereValgFelt("", listOf(VerdiFelt(Hovedytelse.AAP, "AAP")), emptyList()),
                            arbeidOgOpphold = null,
                        ),
                    aktivitet =
                        ReiseTilSamlingAktivitetAvsnitt(
                            aktiviteter =
                                EnumFlereValgFelt(
                                    label = "Hvilken aktivitet søker du støtte til?",
                                    verdier = listOf(VerdiFelt("1", "Tiltak: 12. februar 2026 - 12. mars 2026")),
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
                                    label = "Mottar du lønn gjennom tiltaket?",
                                    verdi = JaNei.NEI,
                                    svarTekst = "Nei",
                                    alternativer = emptyList(),
                                ),
                            tilleggsopplysningerAnnenAktivitet = null,
                            annenAktivitetTypeUtdanning = null,
                        ),
                    samlinger =
                        listOf(
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
                                                verdier = listOf(VerdiFelt(Transportmiddel.DROSJE, "Drosje")),
                                                alternativer = emptyList(),
                                            ),
                                        unntakFraOffentligTransport = null,
                                        unntakFraPrivatBil = null,
                                        offentligTransport = null,
                                        privatBil =
                                            PrivatBilInfo(
                                                benyttetEgenBil = null,
                                                betalteForReisen = null,
                                                infoBilKunDelerAvStrekning = null,
                                                utgifterPrivatBil = null,
                                            ),
                                        drosje =
                                            DrosjeInfo(
                                                harTTKort =
                                                    EnumFelt(
                                                        label = "Har du TT-kort?",
                                                        verdi = JaNei.NEI,
                                                        svarTekst = "Nei",
                                                        alternativer = emptyList(),
                                                    ),
                                            ),
                                    ),
                            ),
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
                                                verdier = listOf(VerdiFelt(Transportmiddel.OFFENTLIG_TRANSPORT, "Offentlig transport")),
                                                alternativer = emptyList(),
                                            ),
                                        unntakFraOffentligTransport = null,
                                        unntakFraPrivatBil = null,
                                        offentligTransport =
                                            OffentligTransportInfo(
                                                totalUtgifterOffentligTransport = VerdiFelt(label = "Totale utgifter", verdi = "450"),
                                            ),
                                        privatBil = null,
                                        drosje = null,
                                    ),
                            ),
                        ),
                    avreiseadresse =
                        AvreiseadresseAvsnitt(
                            skalReiseFraFolkeregistrertAdresse =
                                EnumFelt(
                                    label = "Reiser du fra din folkeregistrerte adresse?",
                                    verdi = JaNei.JA,
                                    svarTekst = "Ja",
                                    alternativer = emptyList(),
                                ),
                            adresseDetSkalReisesFra =
                                Adresse(
                                    land = SelectFelt("Land", "NO", "Norge"),
                                    gateadresse = VerdiFelt(verdi = "Lurendreiergata 1", label = "Gateadresse"),
                                    postnummer = VerdiFelt(verdi = "5132", label = "Postnummer"),
                                    poststed = VerdiFelt(verdi = "Pæddekummen", label = "Poststed"),
                                ),
                        ),
                    dokumentasjon = emptyList(),
                ),
        )
}
