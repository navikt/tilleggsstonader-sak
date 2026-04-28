package no.nav.tilleggsstonader.sak.behandling.opprettelse.dummy

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.søknad.AdresseFelt
import no.nav.tilleggsstonader.kontrakter.søknad.DatoFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFlereValgFelt
import no.nav.tilleggsstonader.kontrakter.søknad.HeltallFelt
import no.nav.tilleggsstonader.kontrakter.søknad.InnsendtSkjema
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.PeriodeFelt
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaReiseTilSamling
import no.nav.tilleggsstonader.kontrakter.søknad.TekstFelt
import no.nav.tilleggsstonader.kontrakter.søknad.VerdiFelt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.AktivitetAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.AnnenAktivitetType
import no.nav.tilleggsstonader.kontrakter.søknad.felles.HovedytelseAvsnitt
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OpprettDummyBehandlingReiseTilSamling(
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
                                PeriodeFelt(
                                    label = "Samling 1",
                                    fra = DatoFelt("Fra", 12 februar 2026),
                                    til = DatoFelt("Til", 14 februar 2026),
                                ),
                                PeriodeFelt(
                                    label = "Samling 2",
                                    fra = DatoFelt("Fra", 10 mars 2026),
                                    til = DatoFelt("Til", 12 mars 2026),
                                ),
                            ),
                        oppmøteadresse =
                            AdresseFelt(
                                label = "Hvor skal du møte opp?",
                                gateadresse = TekstFelt("Gateadresse", "Mimes vei 1"),
                                postnummer = HeltallFelt("Postnummer", 5132),
                                poststed = TekstFelt("Poststed", "Nyborg"),
                            ),
                        kanReiseKollektivt =
                            EnumFelt(
                                label = "Kan du reise kollektivt til samlingen?",
                                verdi = JaNei.NEI,
                                svarTekst = "Nei",
                                alternativer = emptyList(),
                            ),
                        totalbeløpKollektivt = null,
                        årsakIkkeKollektivt =
                            EnumFelt(
                                label = "Hvorfor kan du ikke reise kollektivt?",
                                verdi = SøknadsskjemaReiseTilSamling.ÅrsakIkkeKollektivt.DÅRLIG_TRANSPORTTILBUD,
                                svarTekst = "Dårlig transporttilbud",
                                alternativer = emptyList(),
                            ),
                        kanBenytteEgenBil =
                            EnumFelt(
                                label = "Kan du benytte egen bil?",
                                verdi = JaNei.NEI,
                                svarTekst = "Nei",
                                alternativer = emptyList(),
                            ),
                        årsakIkkeEgenBil =
                            EnumFelt(
                                label = "Hvorfor kan du ikke benytte egen bil?",
                                verdi = SøknadsskjemaReiseTilSamling.ÅrsakIkkeEgenBil.DISPONERER_IKKE_BIL,
                                svarTekst = "Jeg disponerer ikke bil",
                                alternativer = emptyList(),
                            ),
                        kanBenytteDrosje =
                            EnumFelt(
                                label = "Kan du benytte drosje?",
                                verdi = JaNei.JA,
                                svarTekst = "Ja",
                                alternativer = emptyList(),
                            ),
                        dokumentasjon = emptyList(),
                    ),
            )

        val journalpost = Journalpost("TESTJPID", Journalposttype.I, Journalstatus.FERDIGSTILT)
        søknadService.lagreSøknad(behandling.id, journalpost, skjemaReiseTilSamling)
    }
}
