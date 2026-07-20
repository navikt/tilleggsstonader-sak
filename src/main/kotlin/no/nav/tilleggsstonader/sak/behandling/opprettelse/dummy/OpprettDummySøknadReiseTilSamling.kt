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
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaReiseTilSamling
import no.nav.tilleggsstonader.kontrakter.søknad.VerdiFelt
import no.nav.tilleggsstonader.kontrakter.søknad.felles.AnnenAktivitetType
import no.nav.tilleggsstonader.kontrakter.søknad.felles.HovedytelseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.Adresse
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.AvreiseadresseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.KanBenytteEgenBil
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.KanIkkeBenytteEgenBilBegrunnelser
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.KanIkkeReiseMedOffentligTransportBegrunnelser
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.ReiseTilSamlingAktivitetAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.ReisemåteAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.Samling
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OpprettDummySøknadReiseTilSamling(
    private val søknadService: SøknadService,
) {
    fun opprettDummy(
        fagsak: Fagsak,
        behandling: Behandling,
    ) {
        val skjemaReiseTilSamling =
            InnsendtSkjema(
                ident = fagsak.hentAktivIdent(),
                mottattTidspunkt = LocalDateTime.now(),
                språk = Språkkode.NB,
                skjema =
                    SøknadsskjemaReiseTilSamling(
                        hovedytelse =
                            HovedytelseAvsnitt(
                                hovedytelse =
                                    EnumFlereValgFelt(
                                        "",
                                        listOf(VerdiFelt(Hovedytelse.AAP, "AAP")),
                                        emptyList(),
                                    ),
                                arbeidOgOpphold = null,
                            ),
                        aktivitet =
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
                                adresseDetSkalReisesFra = null,
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
                                kanIkkeReiseMedOffentligTransportBegrunnelser =
                                    EnumFlereValgFelt(
                                        label = "Hvorfor kan du ikke reise kollektivt?",
                                        verdier =
                                            listOf(
                                                VerdiFelt(
                                                    KanIkkeReiseMedOffentligTransportBegrunnelser.DÅRLIG_TRANSPORTTILBUD,
                                                    "Dårlig transporttilbud",
                                                ),
                                            ),
                                        alternativer = emptyList(),
                                    ),
                                kanBenytteEgenBil =
                                    EnumFelt(
                                        label = "Kan du benytte egen bil?",
                                        verdi = KanBenytteEgenBil.NEI,
                                        svarTekst = "Nei",
                                        alternativer = emptyList(),
                                    ),
                                kanIkkeBenytteEgenBilBegrunnelser =
                                    EnumFlereValgFelt(
                                        label = "Hvorfor kan du ikke benytte egen bil?",
                                        verdier =
                                            listOf(
                                                VerdiFelt(
                                                    KanIkkeBenytteEgenBilBegrunnelser.HAR_IKKE_BIL_ELLER_FØRERKORT,
                                                    "Jeg har ikke bil eller førerkort",
                                                ),
                                            ),
                                        alternativer = emptyList(),
                                    ),
                                ønskerDekketUtgifterForDrosje =
                                    EnumFelt(
                                        label = "Ønsker du å få dekket utgifter for drosje?",
                                        verdi = JaNei.NEI,
                                        svarTekst = "Nei",
                                        alternativer = emptyList(),
                                    ),
                                barnehageGateadresse = null,
                                barnehagePostnummer = null,
                                betalerForReiseSelv = null,
                                harTTKort = null,
                                reiseMedBilUtgifter = null,
                            ),
                        dokumentasjon = emptyList(),
                    ),
            )

        val journalpost = Journalpost("TESTJPID", Journalposttype.I, Journalstatus.FERDIGSTILT)
        søknadService.lagreSøknad(behandling.id, journalpost, skjemaReiseTilSamling)
    }
}
