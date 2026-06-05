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
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.AnnenAktivitetType
import no.nav.tilleggsstonader.kontrakter.søknad.felles.HovedytelseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.AktivitetAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.AktivitetsadresseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.ReiseavstandAvsnitt
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
                                hovedytelse = EnumFlereValgFelt("", listOf(VerdiFelt(Hovedytelse.AAP, "AAP")), emptyList()),
                                arbeidOgOpphold = null,
                            ),
                        aktivitet =
                            AktivitetAvsnitt(
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
                                lønnetAktivitet = EnumFelt("Mottar du lønn gjennom tiltaket?", JaNei.NEI, "Nei", emptyList()),
                            ),
                        samlinger =
                            listOf(
                                Samling(
                                    fom = DatoFelt("Fra", 12 februar 2026),
                                    tom = DatoFelt("Til", 14 februar 2026),
                                ),
                                Samling(
                                    fom = DatoFelt("Fra", 10 mars 2026),
                                    tom = DatoFelt("Til", 12 mars 2026),
                                ),
                            ),
                        reiseavstand =
                            ReiseavstandAvsnitt(
                                antallKilometerEnVei = VerdiFelt(verdi = "42", label = "Antall kilometer én vei"),
                                aktivitetsadresse =
                                    AktivitetsadresseAvsnitt(
                                        land = SelectFelt("Land", "NO", "Norge"),
                                        gateadresse = VerdiFelt(verdi = "Mimes vei 1", label = "Gateadresse"),
                                        postnummer = VerdiFelt(verdi = "5132", label = "Postnummer"),
                                        poststed = VerdiFelt(verdi = "Nyborg", label = "Poststed"),
                                    ),
                            ),
                        reisemåte =
                            ReisemåteAvsnitt(
                                kanReiseKollektivt =
                                    EnumFelt(
                                        label = "Kan du reise kollektivt til samlingen?",
                                        verdi = JaNei.NEI,
                                        svarTekst = "Nei",
                                        alternativer = emptyList(),
                                    ),
                                totalutgifterKollektivt = null,
                                kanBenytteEgenBil =
                                    EnumFelt(
                                        label = "Kan du benytte egen bil?",
                                        verdi = JaNei.NEI,
                                        svarTekst = "Nei",
                                        alternativer = emptyList(),
                                    ),
                                kanBenytteDrosje =
                                    EnumFelt(
                                        label = "Kan du benytte drosje?",
                                        verdi = JaNei.JA,
                                        svarTekst = "Ja",
                                        alternativer = emptyList(),
                                    ),
                            ),
                        dokumentasjon = emptyList(),
                    ),
            )

        val journalpost = Journalpost("TESTJPID", Journalposttype.I, Journalstatus.FERDIGSTILT)
        søknadService.lagreSøknad(behandling.id, journalpost, skjemaReiseTilSamling)
    }
}
