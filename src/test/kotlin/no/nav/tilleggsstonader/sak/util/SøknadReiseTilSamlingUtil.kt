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
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.KanBenytteEgenBil
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.ReiseTilSamlingAktivitetAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.ReisemåteAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.Samling
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
                                harBruktEkstraReiseDager =
                                    EnumFelt(
                                        label = "Brukte Ekstra reise?",
                                        verdi = JaNei.NEI,
                                        svarTekst = "Nei",
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
                                harBruktEkstraReiseDager =
                                    EnumFelt(
                                        label = "Brukte Ekstra reise?",
                                        verdi = JaNei.NEI,
                                        svarTekst = "Nei",
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
                    reisemåte =
                        ReisemåteAvsnitt(
                            kanReiseMedOffentligTransport =
                                EnumFelt(
                                    label = "Kan du reise kollektivt til samlingen?",
                                    verdi = JaNei.NEI,
                                    svarTekst = "Nei",
                                    alternativer = emptyList(),
                                ),
                            totalUtgifterOffentligTransport = null,
                            kanBenytteEgenBil =
                                EnumFelt(
                                    label = "Kan du benytte egen bil?",
                                    verdi = KanBenytteEgenBil.NEI,
                                    svarTekst = "Nei",
                                    alternativer = emptyList(),
                                ),
                            kanIkkeReiseMedOffentligTransportBegrunnelser = null,
                            ønskerDekketUtgifterForDrosje =
                                EnumFelt(
                                    label = "Ønsker du å få dekket utgifter for drosje?",
                                    verdi = JaNei.JA,
                                    svarTekst = "Ja",
                                    alternativer = emptyList(),
                                ),
                            barnehageGateadresse = null,
                            barnehagePostnummer = null,
                            kanIkkeBenytteEgenBilBegrunnelser = null,
                            betalerForReiseSelv = null,
                            harTTKort = null,
                            reiseMedBilUtgifter = null,
                        ),
                    dokumentasjon = emptyList(),
                ),
        )
}
